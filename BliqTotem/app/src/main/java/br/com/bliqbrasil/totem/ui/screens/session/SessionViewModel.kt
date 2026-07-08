package br.com.bliqbrasil.totem.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import br.com.bliqbrasil.totem.data.model.Machine
import br.com.bliqbrasil.totem.data.model.machinesForTipo
import br.com.bliqbrasil.totem.data.repository.PosRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SessionViewModel(
    private val repository: PosRepository,
    private val bleManager: BliqBleManager,
    val cicloId: String,
    val totalMinutes: Int,
    val resumeFromSeconds: Int?,
    val boxTipo: String = "LAVACAO",
) : ViewModel() {

    val machines: List<Machine> = machinesForTipo(boxTipo)
    val sessionTitle: String = if (boxTipo == "ASPIRACAO") "Sessão de Aspiração" else "Sessão de Lavagem"
    val serviceLabel: String = if (boxTipo == "ASPIRACAO") "aspiração" else "lavagem"

    data class UiState(
        val remaining: Int = 0,
        val sessionStarted: Boolean = false,
        val activeMachine: Machine? = null,
        val isEnding: Boolean = false,
        val bleError: String? = null,
    )

    private val _state = MutableStateFlow(
        UiState(
            remaining = resumeFromSeconds ?: (totalMinutes * 60),
            sessionStarted = resumeFromSeconds != null,
        )
    )
    val state = _state.asStateFlow()

    private val ended = AtomicBoolean(false)
    private var timerJob: Job? = null
    private var didSyncEsp = false
    private var espHasSession = false

    init {
        if (resumeFromSeconds != null) {
            startTimer()
            viewModelScope.launch {
                repository.iniciarCiclo(cicloId)
            }
        }
        listenForBleConnectToResync()
        startHeartbeat()
    }

    // ── Timer ─────────────────────────────────────────────────────────────

    fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _state.update { s ->
                    val next = (s.remaining - 1).coerceAtLeast(0)
                    s.copy(remaining = next)
                }
                if (_state.value.remaining == 0 && !ended.get()) {
                    finishSession(automatic = true, espInitiated = false)
                    break
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ── BLE ───────────────────────────────────────────────────────────────

    private fun listenForBleConnectToResync() {
        viewModelScope.launch {
            bleManager.status.collect { status ->
                when (status) {
                    ConnectionStatus.DISCONNECTED -> {
                        didSyncEsp = false
                        espHasSession = false
                    }
                    ConnectionStatus.CONNECTED -> {
                        if (!didSyncEsp && _state.value.sessionStarted) {
                            didSyncEsp = true
                            val seconds = maxOf(1, _state.value.remaining)
                            try {
                                bleManager.sendCommand("""{"action":"START","duration":$seconds}""")
                                espHasSession = true
                                _state.value.activeMachine?.let { machine ->
                                    bleManager.sendCommand("""{"action":"SELECT","machine":"${machine.name}"}""")
                                }
                            } catch (_: Exception) {
                                didSyncEsp = false
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun startHeartbeat() {
        viewModelScope.launch {
            while (!ended.get()) {
                delay(5_000)
                val s = _state.value
                if (ended.get() || !s.sessionStarted || s.activeMachine == null || s.isEnding) continue
                if (bleManager.status.value != ConnectionStatus.CONNECTED) continue
                try {
                    val seconds = maxOf(1, s.remaining)
                    bleManager.sendCommand("""{"action":"START","duration":$seconds}""")
                    espHasSession = true
                    bleManager.sendCommand("""{"action":"SELECT","machine":"${s.activeMachine.name}"}""")
                } catch (_: Exception) { }
            }
        }
    }

    // ── User actions ──────────────────────────────────────────────────────

    fun selectMachine(machine: Machine) {
        if (_state.value.isEnding) return
        if (machine == _state.value.activeMachine && _state.value.sessionStarted) return

        viewModelScope.launch {
            _state.update { it.copy(bleError = null) }
            try {
                ensureConnected()
                if (!_state.value.sessionStarted || !espHasSession) {
                    val duration = if (_state.value.sessionStarted)
                        maxOf(1, _state.value.remaining)
                    else
                        totalMinutes * 60
                    bleManager.sendCommand("""{"action":"START","duration":$duration}""")
                    espHasSession = true
                    if (!_state.value.sessionStarted) {
                        _state.update { it.copy(sessionStarted = true) }
                        startTimer()
                    }
                }
                bleManager.sendCommand("""{"action":"SELECT","machine":"${machine.name}"}""")
                _state.update { it.copy(activeMachine = machine) }
            } catch (e: Exception) {
                _state.update { it.copy(bleError = e.message ?: "Erro ao enviar comando.") }
            }
        }
    }

    fun requestEndSession(): String {
        val remaining = _state.value.remaining
        return if (_state.value.sessionStarted) {
            val m = remaining / 60
            val s = remaining % 60
            "Ainda restam %02d:%02d de $serviceLabel. Deseja encerrar mesmo assim?".format(m, s)
        } else {
            "Deseja cancelar a sessão?"
        }
    }

    fun confirmEndSession() {
        viewModelScope.launch { finishSession(automatic = false, espInitiated = false) }
    }

    // ── Session finish ────────────────────────────────────────────────────

    private suspend fun finishSession(automatic: Boolean, espInitiated: Boolean) {
        if (!ended.compareAndSet(false, true)) return
        _state.update { it.copy(isEnding = true) }
        stopTimer()

        if (!espInitiated && bleManager.status.value == ConnectionStatus.CONNECTED) {
            try { bleManager.sendCommand("""{"action":"STOP"}""") } catch (_: Exception) {}
        }

        withContext(NonCancellable) {
            repository.finalizarCicloComRetry(cicloId)
            repository.registrarTelemetriaInternal(
                "CICLO_FINALIZADO",
                mapOf("cicloId" to cicloId, "automatic" to automatic, "espInitiated" to espInitiated)
            )
        }

        onFinished()
    }

    private var onFinished: () -> Unit = {}

    fun setOnFinished(callback: () -> Unit) {
        onFinished = callback
    }

    private suspend fun ensureConnected() {
        if (bleManager.status.value != ConnectionStatus.CONNECTED) {
            bleManager.waitForConnection()
        }
    }

    companion object {
        fun factory(
            repository: PosRepository,
            bleManager: BliqBleManager,
            cicloId: String,
            totalMinutes: Int,
            resumeFromSeconds: Int?,
            boxTipo: String = "LAVACAO",
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SessionViewModel(repository, bleManager, cicloId, totalMinutes, resumeFromSeconds, boxTipo) as T
            }
        }
    }
}
