package br.com.bliqbrasil.totem.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.bliqbrasil.totem.data.model.PaymentMethod
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CheckoutViewModel(
    val washOption: WashOption,
    val selectedExtras: List<WashOptionExtra>,
    val totalMinutes: Int,
    val totalPrice: Double,
) : ViewModel() {

    data class UiState(val selectedMethod: PaymentMethod? = null)

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun selectMethod(method: PaymentMethod) {
        _state.update { it.copy(selectedMethod = method) }
    }

    companion object {
        fun factory(
            washOption: WashOption,
            selectedExtras: List<WashOptionExtra>,
            totalMinutes: Int,
            totalPrice: Double,
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CheckoutViewModel(washOption, selectedExtras, totalMinutes, totalPrice) as T
            }
        }
    }
}
