package br.com.bliqbrasil.totem.ui.screens.success

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import br.com.bliqbrasil.totem.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BleStep { IDLE, CONNECTING, REGISTERING, SUCCESS, ERROR }

class SuccessViewModel(
    private val repository: PosRepository,
    private val bleManager: BliqBleManager,
    val cicloId: String,
    val totalMinutes: Int,
) : ViewModel() {

    data class UiState(
        val bleStep: BleStep = BleStep.IDLE,
        val bleError: String? = null,
        val sessionReady: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { runBleSequence() }

    fun retry() { runBleSequence() }

    private fun runBleSequence() {
        viewModelScope.launch {
            _state.update { it.copy(bleError = null) }
            try {
                if (bleManager.status.value != ConnectionStatus.CONNECTED) {
                    _state.update { it.copy(bleStep = BleStep.CONNECTING) }
                    bleManager.waitForConnection()
                }

                _state.update { it.copy(bleStep = BleStep.REGISTERING) }
                // getOrThrow: sem isso a falha do backend (ex.: 409 Box offline) era
                // descartada e a tela seguia como se o ciclo tivesse iniciado.
                repository.iniciarCiclo(cicloId).getOrThrow()

                _state.update { it.copy(bleStep = BleStep.SUCCESS) }
                launch { repository.registrarTelemetriaInternal("CICLO_INICIADO", mapOf("cicloId" to cicloId, "totalMinutes" to totalMinutes)) }

                kotlinx.coroutines.delay(600)
                _state.update { it.copy(sessionReady = true) }

            } catch (e: Exception) {
                _state.update { it.copy(bleStep = BleStep.ERROR, bleError = e.message ?: "Erro desconhecido.") }
            }
        }
    }

    companion object {
        fun factory(
            repository: PosRepository,
            bleManager: BliqBleManager,
            cicloId: String,
            totalMinutes: Int,
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SuccessViewModel(repository, bleManager, cicloId, totalMinutes) as T
            }
        }
    }
}
