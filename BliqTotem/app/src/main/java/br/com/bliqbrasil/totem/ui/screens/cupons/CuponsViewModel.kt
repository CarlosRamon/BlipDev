package br.com.bliqbrasil.totem.ui.screens.cupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.data.model.CupomDisponivel
import br.com.bliqbrasil.totem.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Cupons que o cliente pode usar agora neste box. O backend já filtra pela
 * franquia e pelo tipo do box do terminal, pelo cliente identificado e pelos
 * marcados para aparecer no totem.
 */
class CuponsViewModel(
    private val repository: PosRepository,
    private val clienteId: String,
    private val valorBruto: Double,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Loaded(val cupons: List<CupomDisponivel>) : UiState
        data object Error : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    val identificado: Boolean get() = clienteId.isNotBlank()

    init { carregar() }

    fun carregar() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = repository.listarCupons(clienteId.ifBlank { null }, valorBruto)
                .fold(onSuccess = { UiState.Loaded(it) }, onFailure = { UiState.Error })
        }
    }

    companion object {
        fun factory(repository: PosRepository, clienteId: String, valorBruto: Double) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CuponsViewModel(repository, clienteId, valorBruto) as T
                }
            }
    }
}
