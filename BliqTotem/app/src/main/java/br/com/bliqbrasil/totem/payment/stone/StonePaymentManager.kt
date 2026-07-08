package br.com.bliqbrasil.totem.payment.stone

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import br.com.bliqbrasil.totem.BuildConfig
import br.com.bliqbrasil.totem.R
import br.com.stone.posandroid.providers.PosPrintReceiptProvider
import br.com.stone.posandroid.providers.PosTransactionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import stone.application.enums.Action
import stone.application.enums.InstalmentTransactionEnum
import stone.application.enums.ReceiptType
import stone.application.enums.TransactionStatusEnum
import stone.application.enums.TypeOfTransactionEnum
import stone.application.interfaces.StoneActionCallback
import stone.database.transaction.TransactionObject
import stone.providers.ActiveApplicationProvider
import stone.providers.ReversalProvider
import stone.utils.Stone
import java.util.UUID
import kotlin.concurrent.thread

class StonePaymentManager {

    private val TAG = "StonePaymentManager"

    private val _state = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val state: StateFlow<PaymentState> = _state.asStateFlow()

    private var currentProvider: PosTransactionProvider? = null
    private var currentTransaction: TransactionObject? = null

    var lastApprovedTransaction: TransactionObject? = null
        private set

    private val debugScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var debugJob: Job? = null

    // ── Ativação ──────────────────────────────────────────────────────────────

    fun isActivated(): Boolean {
        if (!BuildConfig.STONE_ENABLED) return true
        return runCatching {
            val users = Stone.sessionApplication.userModelList
            !users.isNullOrEmpty()
        }.getOrDefault(false)
    }

    fun activate(context: Context, stoneCode: String, onResult: (Result<Unit>) -> Unit) {
        try {
            val provider = ActiveApplicationProvider(context)
            provider.setConnectionCallback(object : StoneActionCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Stone code ativado: $stoneCode")
                    onResult(Result.success(Unit))
                }

                override fun onError() {
                    Log.e(TAG, "Erro ao ativar Stone code: $stoneCode")
                    onResult(Result.failure(Exception("Falha na ativação do Stone code")))
                }

                override fun onStatusChanged(action: Action?) {
                    Log.d(TAG, "Activation status: $action")
                }
            })
            provider.activate(stoneCode)
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao ativar: ${e.message}")
            onResult(Result.failure(e))
        }
    }

    // ── Pagamento ─────────────────────────────────────────────────────────────

    fun startPayment(
        activity: Activity,
        amountCents: Long,
        type: TransactionType,
        orderId: String,
    ) {
        lastApprovedTransaction = null

        if (!BuildConfig.STONE_ENABLED) {
            debugJob?.cancel()
            debugJob = debugScope.launch {
                _state.value = PaymentState.WaitingCard
                delay(1200)
                _state.value = PaymentState.Sending
                delay(800)
                val fakeKey = "DEBUG${UUID.randomUUID().toString().replace("-", "").take(8).uppercase()}"
                _state.value = PaymentState.Success(
                    StonePaymentResult(
                        success = true,
                        code = "0",
                        message = "Debug: Aprovado",
                        body = "debug=true&amount=$amountCents",
                        acquirerTransactionKey = fakeKey,
                    )
                )
            }
            return
        }

        val users = Stone.sessionApplication?.userModelList
        if (users.isNullOrEmpty()) {
            _state.value = PaymentState.Failure("", "Stone code não ativado")
            return
        }

        val user = if (type == TransactionType.PIX) {
            users.firstOrNull { it.stoneCode == BuildConfig.STONE_CODE } ?: users[0]
        } else {
            users[0]
        }
        Log.d(TAG, "startPayment — type=$type stoneCode=${user.stoneCode} activatedCodes=${users.map { it.stoneCode }}")

        val txn = TransactionObject().apply {
            amount = amountCents.toString()
            typeOfTransaction = when (type) {
                TransactionType.DEBIT  -> TypeOfTransactionEnum.DEBIT
                TransactionType.CREDIT -> TypeOfTransactionEnum.CREDIT
                TransactionType.PIX    -> TypeOfTransactionEnum.PIX
            }
            instalmentTransaction = InstalmentTransactionEnum.ONE_INSTALMENT
            isCapture = true
            initiatorTransactionKey = if (type == TransactionType.PIX) null else orderId
        }
        currentTransaction = txn

        val provider = PosTransactionProvider(activity, txn, user)
        provider.useDefaultUI(false)
        var done = false
        provider.setConnectionCallback(object : StoneActionCallback {
            override fun onSuccess() {
                if (done) return
                done = true
                val status = provider.transactionStatus
                Log.d(TAG, "onSuccess — status=$status acquirerKey=${txn.acquirerTransactionKey}")
                when (status) {
                    TransactionStatusEnum.APPROVED -> {
                        lastApprovedTransaction = txn
                        _state.value = PaymentState.Success(
                            StonePaymentResult(
                                success = true,
                                code = "0",
                                message = "Aprovado",
                                body = buildParamsString(txn),
                                acquirerTransactionKey = txn.acquirerTransactionKey ?: "",
                            )
                        )
                    }
                    TransactionStatusEnum.DECLINED,
                    TransactionStatusEnum.DECLINED_BY_CARD -> {
                        _state.value = PaymentState.Failure(
                            code = txn.acquirerTransactionKey ?: "",
                            message = "Não autorizada",
                        )
                    }
                    TransactionStatusEnum.CANCELLED,
                    TransactionStatusEnum.REVERSED,
                    TransactionStatusEnum.PENDING_REVERSAL -> {
                        _state.value = PaymentState.Cancelled
                    }
                    else -> {
                        _state.value = PaymentState.Failure(
                            code = txn.acquirerTransactionKey ?: "",
                            message = "Erro técnico (status=$status)",
                        )
                    }
                }
            }

            override fun onError() {
                if (done) return
                done = true
                val status = provider.transactionStatus
                val errors = runCatching { provider.listOfErrors }.getOrNull()
                Log.e(TAG, "onError — status=$status acquirerKey=${txn.acquirerTransactionKey} errors=$errors")
                if (status == TransactionStatusEnum.PENDING) {
                    thread(name = "StoneReversal") { runReversal(activity) }
                }
                val message = when (status) {
                    TransactionStatusEnum.DECLINED,
                    TransactionStatusEnum.DECLINED_BY_CARD -> "Não autorizada"
                    TransactionStatusEnum.CANCELLED        -> "Transação cancelada"
                    TransactionStatusEnum.TECHNICAL_ERROR  -> "Erro técnico"
                    TransactionStatusEnum.REJECTED         -> "Não autorizada"
                    TransactionStatusEnum.PENDING          -> "Não autorizada"
                    else                                   -> "Não autorizada"
                }
                _state.value = PaymentState.Failure(
                    code = txn.acquirerTransactionKey ?: "",
                    message = message,
                )
            }

            override fun onStatusChanged(action: Action?) {
                if (done) return
                Log.d(TAG, "Payment status: $action")
                when (action) {
                    Action.TRANSACTION_WAITING_CARD ->
                        _state.value = PaymentState.WaitingCard

                    Action.TRANSACTION_WAITING_QRCODE_SCAN ->
                        _state.value = PaymentState.WaitingQrCode(txn.getQRCode())

                    Action.TRANSACTION_WAITING_PASSWORD ->
                        _state.value = PaymentState.WaitingPassword

                    Action.TRANSACTION_SENDING ->
                        _state.value = PaymentState.Sending

                    Action.TRANSACTION_REMOVE_CARD ->
                        _state.value = PaymentState.WaitingRemoveCard

                    else -> Unit
                }
            }
        })
        currentProvider = provider
        thread(name = "StoneTransaction") { provider.execute() }
    }

    fun cancelPayment() {
        debugJob?.cancel()
        runCatching { currentProvider?.abortPayment() }
        _state.value = PaymentState.Cancelled
        currentProvider = null
        currentTransaction = null
    }

    fun resetState() {
        debugJob?.cancel()
        _state.value = PaymentState.Idle
        currentProvider = null
        currentTransaction = null
        // lastApprovedTransaction preservado para impressão após navegação
    }

    // ── Impressão ─────────────────────────────────────────────────────────────

    fun printClientReceipt(activity: Activity, onDone: () -> Unit) {
        val txn = lastApprovedTransaction ?: run {
            Log.w(TAG, "printClientReceipt: nenhuma transação aprovada disponível")
            onDone()
            return
        }

        try {
            val logo = runCatching {
                BitmapFactory.decodeResource(activity.resources, R.mipmap.ic_launcher)
            }.getOrNull()

            val provider = if (logo != null) {
                PosPrintReceiptProvider(activity, txn, ReceiptType.CLIENT, logo)
            } else {
                PosPrintReceiptProvider(activity, txn, ReceiptType.CLIENT)
            }

            provider.useDefaultUI(false)
            provider.setConnectionCallback(object : StoneActionCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Impressão concluída")
                    onDone()
                }

                override fun onError() {
                    Log.e(TAG, "Impressão falhou (onError)")
                    onDone()
                }

                override fun onStatusChanged(action: Action?) {
                    Log.d(TAG, "Print status: $action")
                }
            })
            provider.execute()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar impressão: ${e.message}")
            onDone()
        }
    }

    // ── Reversal ──────────────────────────────────────────────────────────────

    fun runReversal(context: Context) {
        if (!isActivated()) return
        try {
            val provider = ReversalProvider(context)
            provider.setConnectionCallback(object : StoneActionCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Reversal concluído")
                }
                override fun onError() {
                    Log.e(TAG, "Reversal falhou")
                }
                override fun onStatusChanged(action: Action?) {}
            })
            thread(name = "StoneReversal") { provider.execute() }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção no reversal: ${e.message}")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildParamsString(txn: TransactionObject): String = buildString {
        append("acquirerTransactionKey=${txn.acquirerTransactionKey}")
        append("&amount=${txn.amount}")
        append("&typeOfTransaction=${txn.typeOfTransaction}")
    }
}
