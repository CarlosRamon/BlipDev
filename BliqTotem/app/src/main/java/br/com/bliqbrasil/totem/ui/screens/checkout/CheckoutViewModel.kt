package br.com.bliqbrasil.totem.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.data.model.PaymentMethod
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val repository: PosRepository,
    val washOption: WashOption,
    val selectedExtras: List<WashOptionExtra>,
    val totalMinutes: Int,
    /** Valor de tabela (pacote + extras), antes de qualquer desconto. */
    val totalPrice: Double,
    private val clienteId: String,
    /** Compra pela oferta do fim do ciclo: não aceita cupom. */
    val crossSell: Boolean,
) : ViewModel() {

    /** Desconto e total a pagar vêm calculados pelo backend na validação. */
    data class CupomAplicado(
        val codigo: String,
        val descricao: String?,
        val desconto: Double,
        val valorFinal: Double,
    )

    data class UiState(
        val selectedMethod: PaymentMethod? = null,
        val codigoDigitado: String = "",
        val validandoCupom: Boolean = false,
        val erroCupom: String? = null,
        val cupom: CupomAplicado? = null,
    ) {
        fun valorAPagar(totalPrice: Double) = cupom?.valorFinal ?: totalPrice
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    val aceitaCupom: Boolean get() = !crossSell

    fun selectMethod(method: PaymentMethod) {
        _state.update { it.copy(selectedMethod = method) }
    }

    fun updateCodigo(texto: String) {
        val codigo = texto.uppercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(30)
        _state.update { it.copy(codigoDigitado = codigo, erroCupom = null) }
    }

    /** Valida no backend e, se aceito, aplica. Serve ao campo e à lista de cupons. */
    fun aplicarCupom(codigo: String = _state.value.codigoDigitado) {
        if (!aceitaCupom || codigo.isBlank() || _state.value.validandoCupom) return
        _state.update { it.copy(validandoCupom = true, erroCupom = null, codigoDigitado = codigo) }
        viewModelScope.launch {
            val result = repository.validarCupom(codigo, totalPrice, clienteId.ifBlank { null })
            val resp = result.getOrNull()
            _state.update {
                when {
                    resp == null -> it.copy(
                        validandoCupom = false,
                        erroCupom = "Não foi possível validar o cupom. Tente de novo.",
                    )
                    !resp.ok -> it.copy(
                        validandoCupom = false,
                        erroCupom = resp.mensagem ?: "Cupom inválido",
                    )
                    else -> it.copy(
                        validandoCupom = false,
                        codigoDigitado = "",
                        cupom = CupomAplicado(
                            codigo     = resp.codigo ?: codigo,
                            descricao  = resp.descricao,
                            desconto   = resp.desconto ?: 0.0,
                            valorFinal = resp.valorFinal ?: totalPrice,
                        ),
                    )
                }
            }
        }
    }

    fun removerCupom() {
        _state.update { it.copy(cupom = null, erroCupom = null) }
    }

    companion object {
        fun factory(
            repository: PosRepository,
            washOption: WashOption,
            selectedExtras: List<WashOptionExtra>,
            totalMinutes: Int,
            totalPrice: Double,
            clienteId: String,
            crossSell: Boolean,
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CheckoutViewModel(
                    repository, washOption, selectedExtras, totalMinutes, totalPrice, clienteId, crossSell,
                ) as T
            }
        }
    }
}
