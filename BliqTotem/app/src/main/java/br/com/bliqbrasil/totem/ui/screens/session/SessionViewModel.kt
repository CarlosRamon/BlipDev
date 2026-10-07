package br.com.bliqbrasil.totem.ui.screens.session

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import br.com.bliqbrasil.totem.data.model.Machine
import br.com.bliqbrasil.totem.data.model.machinesForTipo
import br.com.bliqbrasil.totem.data.repository.PosRepository
import br.com.bliqbrasil.totem.diagnostics.Categoria
import br.com.bliqbrasil.totem.diagnostics.DiagnosticLogger
import br.com.bliqbrasil.totem.diagnostics.Severidade
import org.json.JSONObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SessionViewModel(
    private val repository: PosRepository,
    val bleManager: BliqBleManager,
    val cicloId: String,
    val totalMinutes: Int,
    val resumeFromSeconds: Int?,
    val boxTipo: String = "LAVACAO",
    private val clienteId: String = "",
    private val crossSellMinutos: Int? = null,
    private val crossSellPreco: Double? = null,
    private val diagnostico: DiagnosticLogger,
) : ViewModel() {

    val machines: List<Machine> = machinesForTipo(boxTipo)
    val sessionTitle: String = if (boxTipo == "ASPIRACAO") "Sessão de Aspiração" else "Sessão de Lavagem"
    val serviceLabel: String = if (boxTipo == "ASPIRACAO") "aspiração" else "lavagem"

    data class UiState(
        val remaining: Int = 0,
        val sessionStarted: Boolean = false,
        val activeMachine: Machine? = null,
        val isEnding: Boolean = false,
        val isPaused: Boolean = false,
        val bleError: String? = null,
        // Oferta de minutos extras no fim do ciclo. Não-nula apenas enquanto a
        // contagem regressiva está na tela.
        val offer: Offer? = null,
    )

    data class Offer(
        val minutos: Int,
        val preco: Double,
        val produtoAvulsoId: String,
        val secondsLeft: Int,
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
        diagnostico.cicloId = cicloId
        if (resumeFromSeconds != null) {
            startTimer()
            viewModelScope.launch {
                repository.iniciarCiclo(cicloId)
            }
        }
        listenForBleConnectToResync()
        listenForClpNotifications()
        registrarPausas()
        startHeartbeat()
        prefetchProdutoAvulso()
    }

    // ── Cross-sell ────────────────────────────────────────────────────────

    /**
     * O id do produto de minutagem avulsa do box, resolvido no começo da sessão.
     * Buscar isso só no fim seria pedir para a oferta não aparecer justamente
     * quando a rede falha — e são 30 segundos, sem tempo para uma ida à API.
     */
    private var produtoAvulsoId: String? = null
    private var offerJob: Job? = null

    private fun prefetchProdutoAvulso() {
        if (crossSellMinutos == null || crossSellPreco == null) return
        viewModelScope.launch {
            repository.getConfig().getOrNull()?.let { config ->
                produtoAvulsoId = config.produtos
                    .firstOrNull { it.tipo == "MINUTAGEM_AVULSA" }
                    ?.id
            }
        }
    }

    private fun startOffer(minutos: Int, preco: Double, produtoId: String) {
        _state.update {
            it.copy(offer = Offer(minutos, preco, produtoId, secondsLeft = OFFER_SECONDS))
        }
        offerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val restante = (_state.value.offer?.secondsLeft ?: 0) - 1
                if (restante <= 0) {
                    // Ninguém respondeu: a oferta sai sozinha e o totem volta ao início.
                    _state.update { it.copy(offer = null) }
                    onFinished()
                    break
                }
                _state.update { s -> s.copy(offer = s.offer?.copy(secondsLeft = restante)) }
            }
        }
    }

    fun declineOffer() {
        offerJob?.cancel()
        _state.update { it.copy(offer = null) }
        onFinished()
    }

    fun acceptOffer() {
        val offer = _state.value.offer ?: return
        offerJob?.cancel()
        _state.update { it.copy(offer = null) }
        onAcceptOffer(offer)
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

    // ── Diagnóstico ───────────────────────────────────────────────────────

    /**
     * Sessão pausada = cliente parado no box esperando a ESP voltar. A duração
     * da pausa é o impacto real da queda de conexão, então vai para o registro.
     */
    private fun registrarPausas() {
        viewModelScope.launch {
            var pausadaDesde: Long? = null
            state.map { it.isPaused }.distinctUntilChanged().collect { pausada ->
                val agora = SystemClock.elapsedRealtime()
                if (pausada) {
                    pausadaDesde = agora
                    diagnostico.log(
                        Categoria.BLE, "SESSAO_PAUSADA", Severidade.ERRO,
                        "Sessão pausada: conexão com a ESP32 perdida",
                        mapOf("restanteSegundos" to _state.value.remaining),
                    )
                } else {
                    pausadaDesde?.let { desde ->
                        diagnostico.log(
                            Categoria.BLE, "SESSAO_RETOMADA", Severidade.INFO,
                            "Sessão retomada após ${(agora - desde) / 1000}s",
                            mapOf("pausadaSegundos" to (agora - desde) / 1000, "restanteSegundos" to _state.value.remaining),
                        )
                    }
                    pausadaDesde = null
                }
            }
        }
    }

    override fun onCleared() {
        if (diagnostico.cicloId == cicloId) diagnostico.cicloId = null
    }

    // ── BLE ───────────────────────────────────────────────────────────────

    private fun listenForBleConnectToResync() {
        viewModelScope.launch {
            bleManager.status.collect { status ->
                when (status) {
                    ConnectionStatus.DISCONNECTED,
                    ConnectionStatus.RECONNECTING -> {
                        didSyncEsp = false
                        espHasSession = false
                        if (_state.value.sessionStarted && !_state.value.isEnding && !_state.value.isPaused) {
                            stopTimer()
                            _state.update { it.copy(activeMachine = null, isPaused = true) }
                        }
                    }
                    ConnectionStatus.CONNECTED -> {
                        if (_state.value.sessionStarted && !_state.value.isEnding) {
                            // Libera UI imediatamente — não espera STATUS do ESP para desbloquear
                            _state.update { it.copy(isPaused = false) }
                            if (!didSyncEsp) {
                                viewModelScope.launch {
                                    try { bleManager.sendCommand("""{"action":"STATUS"}""") } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun listenForClpNotifications() {
        viewModelScope.launch {
            bleManager.notification.collect { json ->
                if (_state.value.isEnding) return@collect
                try {
                    val obj = JSONObject(json)
                    if (obj.optString("status") != "STATUS") return@collect

                    val active    = obj.optBoolean("active", false)
                    val remaining = obj.optInt("remaining", 0)
                    val paused    = obj.optBoolean("paused", false)
                    val machName  = obj.optString("machine", "NONE")

                    didSyncEsp = true

                    if (!active || remaining <= 0) {
                        // ESP não tem sessão ativa — libera UI para o usuário tocar uma máquina
                        espHasSession = false
                        _state.update { it.copy(isPaused = false) }
                        return@collect
                    }

                    espHasSession = true
                    _state.update { it.copy(remaining = remaining) }
                    val machine = Machine.values().firstOrNull { it.name == machName }

                    if (!paused) {
                        // ESP está rodando — sincroniza estado e retoma timer local
                        _state.update { it.copy(activeMachine = machine, isPaused = false) }
                        startTimer()
                        return@collect
                    }

                    // ESP com sessão pausada — envia RESUME (machine pode ser null se nenhuma foi selecionada)
                    try {
                        bleManager.sendCommand("""{"action":"RESUME"}""")
                        _state.update { it.copy(activeMachine = machine, isPaused = false) }
                        startTimer()
                    } catch (e: Exception) {
                        _state.update { it.copy(bleError = e.message ?: "Erro ao retomar sessão.") }
                    }
                } catch (_: Exception) {}
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
                    // SELECT não é enviado aqui — a ESP guarda o activeMachine internamente.
                    // Enviar SELECT no heartbeat causava race condition com selectMachine()
                    // e sobrescrevia a seleção do usuário com o snapshot antigo.
                } catch (_: Exception) { }
            }
        }
    }

    // ── User actions ──────────────────────────────────────────────────────

    fun selectMachine(machine: Machine) {
        if (_state.value.isEnding) return
        if (machine == _state.value.activeMachine && _state.value.sessionStarted && timerJob?.isActive == true) return

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
                    }
                }
                bleManager.sendCommand("""{"action":"SELECT","machine":"${machine.name}"}""")
                _state.update { it.copy(activeMachine = machine, isPaused = false) }
                startTimer() // sempre tenta iniciar (guard interno evita duplo início)
            } catch (e: Exception) {
                Log.e("SessionVM", "selectMachine erro [status=${bleManager.status.value}]: ${e.message}")
                diagnostico.log(
                    Categoria.BLE, "SESSAO_MAQUINA_FALHOU", Severidade.ERRO,
                    "Cliente tocou em ${machine.name} e o equipamento não foi acionado: ${e.message}",
                    mapOf("maquina" to machine.name, "erro" to e.message),
                )
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
            if (!repository.finalizarCicloComRetry(cicloId)) {
                diagnostico.log(
                    Categoria.API, "CICLO_NAO_FINALIZADO", Severidade.ERRO,
                    "Não foi possível finalizar o ciclo no servidor após 4 tentativas",
                )
            }
            repository.registrarTelemetriaInternal(
                "CICLO_FINALIZADO",
                mapOf("cicloId" to cicloId, "automatic" to automatic, "espInitiated" to espInitiated)
            )
        }

        // A oferta só faz sentido quando o pacote chegou ao fim por tempo: quem
        // encerrou na mão, ou teve a sessão interrompida pelo equipamento, está
        // indo embora.
        val produtoId = produtoAvulsoId
        if (automatic && !espInitiated && crossSellMinutos != null && crossSellPreco != null && produtoId != null) {
            startOffer(crossSellMinutos, crossSellPreco, produtoId)
        } else {
            onFinished()
        }
    }

    private var onFinished: () -> Unit = {}
    private var onAcceptOffer: (Offer) -> Unit = {}

    fun setOnFinished(callback: () -> Unit) {
        onFinished = callback
    }

    fun setOnAcceptOffer(callback: (Offer) -> Unit) {
        onAcceptOffer = callback
    }

    val clienteIdAtual: String get() = clienteId

    private suspend fun ensureConnected() {
        if (bleManager.status.value != ConnectionStatus.CONNECTED) {
            bleManager.waitForConnection()
        }
    }

    companion object {
        /** Tempo que a oferta fica na tela antes de sumir sozinha. */
        const val OFFER_SECONDS = 30

        fun factory(
            repository: PosRepository,
            bleManager: BliqBleManager,
            cicloId: String,
            totalMinutes: Int,
            resumeFromSeconds: Int?,
            boxTipo: String = "LAVACAO",
            clienteId: String = "",
            crossSellMinutos: Int? = null,
            crossSellPreco: Double? = null,
            diagnostico: DiagnosticLogger,
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SessionViewModel(
                    repository, bleManager, cicloId, totalMinutes, resumeFromSeconds, boxTipo,
                    clienteId, crossSellMinutos, crossSellPreco, diagnostico,
                ) as T
            }
        }
    }
}
