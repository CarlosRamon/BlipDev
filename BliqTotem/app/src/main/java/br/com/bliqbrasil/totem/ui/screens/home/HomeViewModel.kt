package br.com.bliqbrasil.totem.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.BliqTotemApp
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.local.TokenStorage
import br.com.bliqbrasil.totem.data.model.PosConfig
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.data.repository.PosRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel(
    application: Application,
    private val repository: PosRepository,
    val bleManager: BliqBleManager,
    private val tokenStorage: TokenStorage,
) : AndroidViewModel(application) {

    data class UiState(
        val config: PosConfig? = null,
        val washOptions: List<WashOption> = emptyList(),
        val boxTipo: String = "LAVACAO",
        val loading: Boolean = true,
        val error: String? = null,
        val needsActivation: Boolean = false,
        val activeSession: ActiveSession? = null,
    )

    data class ActiveSession(
        val cicloId: String,
        val totalMinutes: Int,
        val resumeFromSeconds: Int? = null,
        val boxTipo: String = "LAVACAO",
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        load()
        startHeartbeat()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, activeSession = null) }

            if (tokenStorage.getToken() == null) {
                _state.update { it.copy(loading = false, needsActivation = true) }
                return@launch
            }

            repository.getConfig().fold(
                onSuccess = { config ->
                    val svcUuid  = config.esp32.serviceUuid ?: config.esp32.uuid
                    val chrUuid  = config.esp32.charUuid    ?: config.esp32.uuid
                    if (svcUuid != null && chrUuid != null) {
                        bleManager.setUuids(svcUuid, chrUuid)
                    }
                    config.esp32.uuid?.let { bleManager.setDeviceName(it) }
                    val options = config.produtos.map { p ->
                        WashOption(
                            id = p.id,
                            label = p.nome,
                            minutes = p.tempoMinutos ?: 1,
                            price = p.preco.toDoubleOrNull() ?: 0.0,
                            tipo = p.tipo,
                            // Só leva a oferta adiante quando ela está completa:
                            // o totem nunca deve exibir "mais null minutos".
                            crossSellMinutos = p.crossSellMinutos.takeIf { p.crossSellAtivo },
                            crossSellPreco = p.crossSellPreco?.toDoubleOrNull().takeIf { p.crossSellAtivo },
                            extras = p.extras.map { e ->
                                WashOptionExtra(
                                    id = e.id,
                                    rotulo = e.rotulo,
                                    minutos = e.minutos,
                                    preco = e.preco.toDoubleOrNull() ?: 0.0,
                                )
                            }
                        )
                    }
                    val boxTipo = config.box.tipo
                    tokenStorage.saveBoxTipo(boxTipo)
                    _state.update { it.copy(config = config, washOptions = options, boxTipo = boxTipo) }

                    // Check for active cycle
                    repository.getCicloAtivo().getOrNull()?.let { ciclo ->
                        if (ciclo != null && ciclo.segundosRestantes > 0) {
                            _state.update { s ->
                                s.copy(
                                    loading = false,
                                    activeSession = ActiveSession(
                                        cicloId           = ciclo.id,
                                        totalMinutes      = ciclo.tempoContratado,
                                        resumeFromSeconds = if (ciclo.status == "EM_ANDAMENTO") ciclo.segundosRestantes else null,
                                        boxTipo           = boxTipo,
                                    )
                                )
                            }
                            return@launch
                        }
                    }
                    _state.update { it.copy(loading = false) }
                },
                onFailure = { e ->
                    val msg = e.message ?: "Erro ao carregar"
                    if (msg.contains("401") || msg.contains("Token", ignoreCase = true)) {
                        tokenStorage.clearToken()
                        _state.update { it.copy(loading = false, needsActivation = true) }
                    } else {
                        _state.update { it.copy(loading = false, error = msg) }
                    }
                }
            )
        }
    }

    // O heartbeat roda no escopo do app (HeartbeatLoop); a Home só usa a
    // resposta para retomar uma sessão que o servidor diz estar ativa.
    private fun startHeartbeat() {
        viewModelScope.launch {
            getApplication<BliqTotemApp>().heartbeat.respostas.collect { resposta ->
                resposta.ciclo?.let { ciclo ->
                    if (ciclo.segundosRestantes > 0 && _state.value.activeSession == null && !_state.value.loading) {
                        _state.update { s ->
                            s.copy(
                                activeSession = ActiveSession(
                                    cicloId           = ciclo.id,
                                    totalMinutes      = ciclo.tempoContratado,
                                    resumeFromSeconds = if (ciclo.status == "EM_ANDAMENTO") ciclo.segundosRestantes else null,
                                    boxTipo           = s.boxTipo,
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun consumeActiveSession() {
        _state.update { it.copy(activeSession = null) }
    }

    fun resetTerminal() {
        bleManager.disconnect()
        tokenStorage.clearToken()
        _state.value = UiState(needsActivation = true)
    }

    companion object {
        fun factory(
            application: Application,
            repository: PosRepository,
            bleManager: BliqBleManager,
            tokenStorage: TokenStorage,
        ) = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(application, repository, bleManager, tokenStorage) as T
            }
        }
    }
}
