package br.com.bliqbrasil.totem.data.repository

import br.com.bliqbrasil.totem.BuildConfig
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.local.TokenStorage
import br.com.bliqbrasil.totem.data.model.HeartbeatResponse
import br.com.bliqbrasil.totem.diagnostics.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val HEARTBEAT_INTERVAL_MS = 30_000L

/**
 * Heartbeat do totem para o backend, no escopo do processo.
 *
 * Antes vivia na HomeViewModel e parava sempre que o totem voltava para a tela
 * de boas-vindas — onde ele passa a maior parte do tempo. O backend marcava o
 * terminal como offline sem ele estar, e o "online" do painel não dizia nada.
 */
class HeartbeatLoop(
    private val repository: PosRepository,
    private val bleManager: BliqBleManager,
    private val tokenStorage: TokenStorage,
    private val network: NetworkMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _respostas = MutableSharedFlow<HeartbeatResponse>(extraBufferCapacity = 1)
    /** Respostas bem-sucedidas — a Home usa para retomar uma sessão ativa. */
    val respostas: SharedFlow<HeartbeatResponse> = _respostas.asSharedFlow()

    fun iniciar() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (tokenStorage.getToken() == null) continue
                val rede = network.info()
                repository.heartbeat(bleManager.status.value.name, rede.tipo, rede.sinal, BuildConfig.VERSION_NAME)
                    .onSuccess { _respostas.tryEmit(it) }
            }
        }
    }
}
