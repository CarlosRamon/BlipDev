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
import br.com.bliqbrasil.totem.diagnostics.Categoria
import br.com.bliqbrasil.totem.diagnostics.DiagnosticLogger
import br.com.bliqbrasil.totem.diagnostics.Severidade
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
    private val diagnostico: DiagnosticLogger,
    private val washOption: WashOption,
    private val selectedExtras: List<WashOptionExtra>,
    private val totalMinutes: Int,
    private val totalPrice: Double,
    val paymentMethod: PaymentMethod,
    private val clienteId: String = "",
    private val crossSell: Boolean = false,
    private val cupomCodigo: String? = null,
    private val valorBruto: Double? = null,
) : ViewModel() {

    data class UiState(
        val paymentState: PaymentState = PaymentState.Idle,
        val isCreatingCiclo: Boolean = false,
        val cicloId: String? = null,
        val acquirerKey: String? = null,
        val navigateBack: Boolean = false,
        // Compra zerada por cupom: o backend recusou (cupom deixou de valer ou
        // não cobre mais o total). Nada foi cobrado, então dá para voltar.
        val erroSemCobranca: String? = null,
        // Cartão/PIX aprovado na Stone, mas o backend não registrou o ciclo depois
        // das retentativas. O cliente JÁ FOI COBRADO: a tela precisa de saída.
        val erroRegistro: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var paymentStarted = false
    private var timeoutJob: Job? = null
    private var pagamentoAprovado: PaymentState.Success? = null
    private var registroJob: Job? = null

    companion object {
        private const val PAYMENT_TIMEOUT_MS = 90_000L
        // Espera antes de cada nova tentativa de registrar o ciclo (3 tentativas no total).
        private val ESPERAS_RETENTATIVA_MS = longArrayOf(2_000L, 5_000L)
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

        val type = when (paymentMethod) {
            PaymentMethod.CREDIT -> TransactionType.CREDIT
            PaymentMethod.DEBIT  -> TransactionType.DEBIT
            PaymentMethod.PIX    -> TransactionType.PIX
            PaymentMethod.CUPOM  -> { criarCicloSemCobranca(); return }
        }
        // round: 17.99 * 100 dá 1798.999…, e o toLong() cobraria um centavo a menos.
        val amountCents = Math.round(totalPrice * 100)
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
        _uiState.update { it.copy(navigateBack = false, erroSemCobranca = null) }
        start(activity)
    }

    /** Cupom de 100%: não passa pela Stone; o backend revalida o cupom e cria o ciclo. */
    private fun criarCicloSemCobranca() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingCiclo = true, erroSemCobranca = null) }
            val result = repository.criarCiclo(
                produtoId       = washOption.id,
                tempoContratado = totalMinutes,
                metodoPagamento = "CUPOM",
                valor           = 0.0,
                transacaoId     = null,
                clienteId       = clienteId.ifBlank { null },
                crossSell       = crossSell,
                cupomCodigo     = cupomCodigo,
                valorBruto      = valorBruto,
            )
            result.onSuccess { ciclo ->
                for (extra in selectedExtras) repository.adicionarExtra(ciclo.id, extra.id)
                _uiState.update { it.copy(isCreatingCiclo = false, cicloId = ciclo.id, acquirerKey = "") }
            }.onFailure { ex ->
                val ativo = (ex as? CicloConflictException)?.ativo
                _uiState.update {
                    if (ativo != null) it.copy(isCreatingCiclo = false, cicloId = ativo.id, acquirerKey = "")
                    else it.copy(isCreatingCiclo = false, erroSemCobranca = ex.message ?: "Não foi possível usar o cupom")
                }
            }
        }
    }

    fun cancel() {
        stone.cancelPayment()
    }

    fun consumeNavigateBack() {
        _uiState.update { it.copy(navigateBack = false) }
    }

    private fun onPaymentSuccess(state: PaymentState.Success) {
        timeoutJob?.cancel()
        if (pagamentoAprovado != null) return
        pagamentoAprovado = state
        registrarCicloPago(state)
    }

    /** Botão da tela de erro: tenta registrar de novo o ciclo do pagamento já aprovado (não cobra outra vez). */
    fun tentarRegistrarDeNovo() {
        val state = pagamentoAprovado ?: return
        if (registroJob?.isActive == true) return
        registrarCicloPago(state)
    }

    private fun registrarCicloPago(state: PaymentState.Success) {
        val acquirerKey = state.result.acquirerTransactionKey ?: ""
        val metodoPagamento = when (paymentMethod) {
            PaymentMethod.CREDIT -> "CARTAO_CREDITO"
            PaymentMethod.DEBIT  -> "CARTAO_DEBITO"
            PaymentMethod.PIX    -> "PIX"
            PaymentMethod.CUPOM  -> "CUPOM" // não chega aqui: não passa pela Stone
        }
        registroJob = viewModelScope.launch {
            _uiState.update { it.copy(isCreatingCiclo = true, erroRegistro = null) }
            var ultimoErro: Throwable? = null
            // Retentar é seguro: se uma tentativa criou o ciclo e só a resposta se
            // perdeu, a próxima recebe 409 com o ciclo ativo e segue para ele.
            for (tentativa in 0..ESPERAS_RETENTATIVA_MS.size) {
                if (tentativa > 0) delay(ESPERAS_RETENTATIVA_MS[tentativa - 1])
                val cicloResult = repository.criarCiclo(
                    produtoId       = washOption.id,
                    tempoContratado = totalMinutes,
                    metodoPagamento = metodoPagamento,
                    valor           = totalPrice,
                    transacaoId     = state.result.acquirerTransactionKey,
                    clienteId       = clienteId.ifBlank { null },
                    crossSell       = crossSell,
                    cupomCodigo     = cupomCodigo,
                    valorBruto      = valorBruto,
                )
                val ciclo = cicloResult.getOrNull()
                if (ciclo != null) {
                    for (extra in selectedExtras) {
                        repository.adicionarExtra(ciclo.id, extra.id)
                    }
                    _uiState.update { it.copy(isCreatingCiclo = false, cicloId = ciclo.id, acquirerKey = acquirerKey) }
                    return@launch
                }
                val ex = cicloResult.exceptionOrNull()
                if (ex is CicloConflictException && ex.ativo != null) {
                    // Já existe sessão ativa neste terminal — segue direto para ela.
                    _uiState.update { it.copy(isCreatingCiclo = false, cicloId = ex.ativo.id, acquirerKey = acquirerKey) }
                    return@launch
                }
                ultimoErro = ex
            }
            diagnostico.log(
                Categoria.PAGAMENTO, "CICLO_NAO_REGISTRADO", Severidade.ERRO,
                "Pagamento aprovado sem ciclo: ${ultimoErro?.message}",
                dados = mapOf(
                    "metodo"      to metodoPagamento,
                    "valor"       to totalPrice,
                    "transacaoId" to acquirerKey,
                    "produtoId"   to washOption.id,
                    "tentativas"  to ESPERAS_RETENTATIVA_MS.size + 1,
                ),
            )
            diagnostico.enviarAgora()
            _uiState.update {
                it.copy(isCreatingCiclo = false, erroRegistro = ultimoErro?.message ?: "Não foi possível liberar o box")
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
        private val diagnostico: DiagnosticLogger,
        private val washOption: WashOption,
        private val selectedExtras: List<WashOptionExtra>,
        private val totalMinutes: Int,
        private val totalPrice: Double,
        private val paymentMethod: PaymentMethod,
        private val clienteId: String = "",
        private val crossSell: Boolean = false,
        private val cupomCodigo: String? = null,
        private val valorBruto: Double? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PaymentViewModel(
                stone, repository, diagnostico, washOption, selectedExtras, totalMinutes, totalPrice, paymentMethod,
                clienteId, crossSell, cupomCodigo, valorBruto,
            ) as T
    }
}
