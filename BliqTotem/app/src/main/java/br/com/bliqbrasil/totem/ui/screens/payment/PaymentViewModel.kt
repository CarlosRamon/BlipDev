package br.com.bliqbrasil.totem.ui.screens.payment

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.data.model.PaymentMethod
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.data.repository.CicloConflictException
import br.com.bliqbrasil.totem.data.repository.PosRepository
import br.com.bliqbrasil.totem.payment.stone.PaymentState
import br.com.bliqbrasil.totem.payment.stone.StonePaymentManager
import br.com.bliqbrasil.totem.payment.stone.TransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class PaymentViewModel(
    private val stone: StonePaymentManager,
    private val repository: PosRepository,
    private val washOption: WashOption,
    private val selectedExtras: List<WashOptionExtra>,
    private val totalMinutes: Int,
    private val totalPrice: Double,
    val paymentMethod: PaymentMethod,
    private val clienteId: String = "",
    private val crossSell: Boolean = false,
) : ViewModel() {

    data class UiState(
        val paymentState: PaymentState = PaymentState.Idle,
        val isCreatingCiclo: Boolean = false,
        val cicloId: String? = null,
        val acquirerKey: String? = null,
        val navigateBack: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var paymentStarted = false
    private var timeoutJob: Job? = null

    companion object {
        private const val PAYMENT_TIMEOUT_MS = 90_000L
    }

    init {
        observePaymentState()
    }

    private fun observePaymentState() {
        viewModelScope.launch {
            stone.state.collect { state ->
                _uiState.update { it.copy(paymentState = state) }
                when (state) {
                    is PaymentState.Success   -> onPaymentSuccess(state)
                    is PaymentState.Failure   -> { timeoutJob?.cancel() }
                    is PaymentState.Cancelled -> { timeoutJob?.cancel(); _uiState.update { it.copy(navigateBack = true) } }
                    else                      -> Unit
                }
            }
        }
    }

    fun start(activity: Activity) {
        if (paymentStarted) return
        paymentStarted = true
        stone.resetState()

        val amountCents = (totalPrice * 100).toLong()
        val type = when (paymentMethod) {
            PaymentMethod.CREDIT -> TransactionType.CREDIT
            PaymentMethod.DEBIT  -> TransactionType.DEBIT
            PaymentMethod.PIX    -> TransactionType.PIX
        }
        val orderId = UUID.randomUUID().toString().replace("-", "").take(20)
        stone.startPayment(activity, amountCents, type, orderId)

        timeoutJob = viewModelScope.launch {
            delay(PAYMENT_TIMEOUT_MS)
            val current = _uiState.value.paymentState
            if (current !is PaymentState.Success && current !is PaymentState.Failure && current !is PaymentState.Cancelled) {
                stone.cancelPayment()
            }
        }
    }

    fun retry(activity: Activity) {
        paymentStarted = false
        _uiState.update { it.copy(navigateBack = false) }
        start(activity)
    }

    fun cancel() {
        stone.cancelPayment()
    }

    fun consumeNavigateBack() {
        _uiState.update { it.copy(navigateBack = false) }
    }

    private fun onPaymentSuccess(state: PaymentState.Success) {
        timeoutJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingCiclo = true) }
            val metodoPagamento = when (paymentMethod) {
                PaymentMethod.CREDIT -> "CARTAO_CREDITO"
                PaymentMethod.DEBIT  -> "CARTAO_DEBITO"
                PaymentMethod.PIX    -> "PIX"
            }
            val cicloResult = repository.criarCiclo(
                produtoId       = washOption.id,
                tempoContratado = totalMinutes,
                metodoPagamento = metodoPagamento,
                valor           = totalPrice,
                transacaoId     = state.result.acquirerTransactionKey,
                clienteId       = clienteId.ifBlank { null },
                crossSell       = crossSell,
            )
            if (cicloResult.isFailure) {
                val ex = cicloResult.exceptionOrNull()
                if (ex is CicloConflictException && ex.ativo != null) {
                    // Session already active from a previous payment — navigate to it directly
                    _uiState.update {
                        it.copy(
                            isCreatingCiclo = false,
                            cicloId         = ex.ativo.id,
                            acquirerKey     = state.result.acquirerTransactionKey ?: "",
                        )
                    }
                } else {
                    _uiState.update { it.copy(isCreatingCiclo = false) }
                }
                return@launch
            }
            val ciclo = cicloResult.getOrThrow()
            for (extra in selectedExtras) {
                repository.adicionarExtra(ciclo.id, extra.id)
            }
            _uiState.update {
                it.copy(
                    isCreatingCiclo = false,
                    cicloId         = ciclo.id,
                    acquirerKey     = state.result.acquirerTransactionKey ?: "",
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stone.resetState()
    }

    class Factory(
        private val stone: StonePaymentManager,
        private val repository: PosRepository,
        private val washOption: WashOption,
        private val selectedExtras: List<WashOptionExtra>,
        private val totalMinutes: Int,
        private val totalPrice: Double,
        private val paymentMethod: PaymentMethod,
        private val clienteId: String = "",
        private val crossSell: Boolean = false,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PaymentViewModel(stone, repository, washOption, selectedExtras, totalMinutes, totalPrice, paymentMethod, clienteId, crossSell) as T
    }
}
