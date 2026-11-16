package br.com.bliqbrasil.totem.ui.screens.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.local.TokenStorage
import br.com.bliqbrasil.totem.data.model.CicloAtivo
import br.com.bliqbrasil.totem.data.model.PoliticaCpf
import br.com.bliqbrasil.totem.data.model.PosConfig
import br.com.bliqbrasil.totem.data.repository.PosRepository
import br.com.bliqbrasil.totem.data.repository.aguardarCicloLiberado
import br.com.bliqbrasil.totem.data.repository.retomarDeSegundos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val repository: PosRepository,
    val bleManager: BliqBleManager,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    enum class PingState { IDLE, CONNECTING, CONNECTED, READY }

    data class UiState(
        val config: PosConfig? = null,
        val pingState: PingState = PingState.IDLE,
        val needsActivation: Boolean = false,
        val activeSession: ActiveSession? = null,
        val politicaCpf: PoliticaCpf = PoliticaCpf.OPCIONAL,
    )

    data class ActiveSession(
        val cicloId: String,
        val totalMinutes: Int,
        val resumeFromSeconds: Int? = null,
        val boxTipo: String = "LAVACAO",
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init { loadConfig() }

    private fun loadConfig() {
        if (tokenStorage.getToken() == null) {
            _state.update { it.copy(needsActivation = true) }
            return
        }
        viewModelScope.launch {
            repository.getConfig().onSuccess { config ->
                val svcUuid  = config.esp32.serviceUuid ?: config.esp32.uuid
                val chrUuid  = config.esp32.charUuid    ?: config.esp32.uuid
                if (svcUuid != null && chrUuid != null) bleManager.setUuids(svcUuid, chrUuid)
                config.esp32.uuid?.let { bleManager.setDeviceName(it) }
                tokenStorage.saveBoxTipo(config.box.tipo)
                tokenStorage.savePoliticaCpf(config.franqueado.politicaCpf)
                _state.update {
                    it.copy(config = config, politicaCpf = PoliticaCpf.from(config.franqueado.politicaCpf))
                }

                // Sessão ativa: pula welcome/CPF e vai direto pra Session
                verificarCicloAtivo(config.box.tipo)
            }.onFailure { e ->
                val msg = e.message ?: ""
                if (msg.contains("401") || msg.contains("Token", ignoreCase = true)) {
                    tokenStorage.clearToken()
                    _state.update { it.copy(needsActivation = true) }
                }
            }
        }
    }

    private suspend fun verificarCicloAtivo(boxTipo: String) {
        val ciclo = repository.getCicloAtivo().getOrNull() ?: return
        if (ciclo.segundosRestantes > 0) abrirSessao(ciclo, boxTipo)
    }

    private fun abrirSessao(ciclo: CicloAtivo, boxTipo: String) {
        if (_state.value.activeSession != null) return
        _state.update {
            it.copy(
                activeSession = ActiveSession(
                    cicloId           = ciclo.id,
                    totalMinutes      = ciclo.tempoContratado,
                    resumeFromSeconds = ciclo.retomarDeSegundos,
                    boxTipo           = boxTipo,
                )
            )
        }
    }

    /**
     * Sem isto a Welcome só consultava ao abrir, e o brinde/manutenção liberado
     * pelo painel só aparecia depois de o cliente ir para o CPF/Home e voltar.
     * Roda enquanto a tela está visível (ver WelcomeScreen).
     */
    suspend fun vigiarCicloLiberado() {
        while (true) {
            val ciclo = repository.aguardarCicloLiberado(
                // Só depois do /config, e não no meio do "iniciar" do cliente.
                podeVerificar = { _state.value.config != null && _state.value.pingState == PingState.IDLE },
            )
            _state.value.config?.let { abrirSessao(ciclo, it.box.tipo) }
        }
    }

    fun consumeActiveSession() {
        _state.update { it.copy(activeSession = null) }
    }

    /**
     * Chamado quando o cliente toca "iniciar". Aguarda BLE conectar (com timeout),
     * mostra o check verde e sinaliza READY para o Screen navegar.
     */
    fun onStart() {
        viewModelScope.launch {
            _state.update { it.copy(pingState = PingState.CONNECTING) }
            try {
                bleManager.waitForConnection(timeoutMs = 15_000L)
            } catch (_: Exception) {
                // segue mesmo sem BLE — cliente pode identificar e escolher produto;
                // a conexão volta a ser exigida antes de liberar a sessão.
            }
            _state.update { it.copy(pingState = PingState.CONNECTED) }
            delay(900)
            _state.update { it.copy(pingState = PingState.READY) }
        }
    }

    fun consumeReady() {
        _state.update { it.copy(pingState = PingState.IDLE) }
    }

    fun resetTerminal() {
        bleManager.disconnect()
        tokenStorage.clearToken()
        _state.value = UiState(needsActivation = true)
    }

    companion object {
        fun factory(
            repository: PosRepository,
            bleManager: BliqBleManager,
            tokenStorage: TokenStorage,
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return WelcomeViewModel(repository, bleManager, tokenStorage) as T
            }
        }
    }
}
