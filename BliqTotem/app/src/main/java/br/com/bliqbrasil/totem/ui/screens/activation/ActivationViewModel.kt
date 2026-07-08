package br.com.bliqbrasil.totem.ui.screens.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.data.local.TokenStorage
import br.com.bliqbrasil.totem.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivationViewModel(
    private val repository: PosRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    data class UiState(
        val code: String = "",
        val loading: Boolean = false,
        val error: String? = null,
        val activated: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun updateCode(code: String) {
        _state.update { it.copy(code = code.uppercase(), error = null) }
    }

    fun activate() {
        val code = _state.value.code.trim()
        if (code.length != 8) {
            _state.update { it.copy(error = "O código deve ter exatamente 8 caracteres") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.ativarPos(code).fold(
                onSuccess = { response ->
                    tokenStorage.saveToken(response.token)
                    _state.update { it.copy(loading = false, activated = true) }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Código inválido ou expirado") }
                }
            )
        }
    }

    companion object {
        fun factory(repository: PosRepository, tokenStorage: TokenStorage) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ActivationViewModel(repository, tokenStorage) as T
                }
            }
    }
}
