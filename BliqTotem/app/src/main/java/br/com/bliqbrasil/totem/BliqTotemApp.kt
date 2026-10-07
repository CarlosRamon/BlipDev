package br.com.bliqbrasil.totem

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.local.TokenStorage
import br.com.bliqbrasil.totem.data.network.NetworkClient
import br.com.bliqbrasil.totem.data.repository.HeartbeatLoop
import br.com.bliqbrasil.totem.data.repository.PosRepository
import br.com.bliqbrasil.totem.diagnostics.Categoria
import br.com.bliqbrasil.totem.diagnostics.DiagnosticLogger
import br.com.bliqbrasil.totem.diagnostics.NetworkMonitor
import br.com.bliqbrasil.totem.diagnostics.Severidade
import br.com.bliqbrasil.totem.payment.stone.StonePaymentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import stone.application.StoneStart
import stone.utils.Stone
import stone.utils.keys.StoneKeyType

class BliqTotemApp : Application() {

    val tokenStorage: TokenStorage by lazy { TokenStorage(this) }

    val diagnostico: DiagnosticLogger by lazy {
        DiagnosticLogger(this, api = { networkClient.api }, temToken = { tokenStorage.getToken() != null })
    }

    val networkClient: NetworkClient by lazy { NetworkClient(tokenStorage) { diagnostico } }

    val repository: PosRepository by lazy { PosRepository(networkClient.api) }

    val bleManager: BliqBleManager by lazy { BliqBleManager(this, diagnostico) }

    val stonePaymentManager: StonePaymentManager by lazy { StonePaymentManager(diagnostico) }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(this, diagnostico) }

    val heartbeat: HeartbeatLoop by lazy { HeartbeatLoop(repository, bleManager, tokenStorage, networkMonitor) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        installThirdPartyCrashFilter()
        iniciarDiagnostico()
        initStoneSDK()
        heartbeat.iniciar()
    }

    private fun iniciarDiagnostico() {
        diagnostico.contexto = {
            mapOf(
                "ble"  to bleManager.status.value.name,
                "rede" to networkMonitor.info().tipo,
            )
        }
        networkMonitor.iniciar()
        diagnostico.iniciar()
        registrarInicio()
    }

    /**
     * Toda subida do app vira um evento, com o motivo pelo qual o processo
     * anterior morreu (Android 11+) e há quanto tempo o aparelho está ligado —
     * aparelho ligado há poucos minutos indica queda de energia no totem.
     */
    private fun registrarInicio() {
        val dados = mutableMapOf<String, Any?>(
            "versionCode"            to BuildConfig.VERSION_CODE,
            "modelo"                 to Build.MODEL,
            "android"                to Build.VERSION.SDK_INT,
            "aparelhoLigadoSegundos" to SystemClock.elapsedRealtime() / 1000,
        )
        var severidade = Severidade.INFO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val prefs = getSharedPreferences("diagnostico", MODE_PRIVATE)
                val am = getSystemService(ActivityManager::class.java)
                val saida = am.getHistoricalProcessExitReasons(packageName, 0, 1).firstOrNull()
                // Só reporta cada encerramento uma vez — o histórico do Android persiste.
                if (saida != null && saida.timestamp > prefs.getLong("ultimaSaidaReportada", 0)) {
                    prefs.edit().putLong("ultimaSaidaReportada", saida.timestamp).apply()
                    val motivo = motivoSaida(saida.reason)
                    dados["encerramentoAnterior"] = motivo
                    dados["encerramentoDescricao"] = saida.description
                    dados["encerramentoHaSegundos"] = (System.currentTimeMillis() - saida.timestamp) / 1000
                    if (motivo in SAIDAS_ANORMAIS) severidade = Severidade.AVISO
                }
            } catch (e: Exception) {
                dados["encerramentoErro"] = e.message
            }
        }
        diagnostico.log(Categoria.APP, "APP_INICIADO", severidade, "App iniciado", dados)
    }

    // Absorve NPEs do SDK da Stone; para crashes do nosso código, reinicia o app automaticamente.
    // Totem de autoatendimento — usuários não sabem agir diante de tela de erro.
    private fun installThirdPartyCrashFilter() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val origin = throwable.stackTrace.firstOrNull()?.className ?: ""
            val isThirdParty = origin.startsWith("stone.") ||
                origin.startsWith("br.com.stone.") ||
                origin.startsWith("br.com.inovare.")
            val dados = mapOf(
                "excecao" to throwable.javaClass.name,
                "thread"  to thread.name,
                "pilha"   to throwable.stackTrace.take(12).joinToString("\n"),
                "causa"   to throwable.cause?.let { "${it.javaClass.name}: ${it.message}" },
            )
            if (isThirdParty) {
                Log.e(TAG, "Exceção de SDK de terceiros absorvida [${thread.name}]: ${throwable.message}", throwable)
                runCatching {
                    diagnostico.log(Categoria.APP, "APP_EXCECAO_SDK", Severidade.AVISO, throwable.message, dados)
                }
            } else {
                Log.e(TAG, "Crash da aplicação — reiniciando [${thread.name}]: ${throwable.message}", throwable)
                runCatching {
                    diagnostico.logImediato(Categoria.APP, "APP_CRASH", Severidade.ERRO, throwable.message, dados)
                }
                restartApp()
            }
        }
    }

    private fun restartApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (intent != null) {
                val pending = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                )
                (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                    .set(AlarmManager.RTC, System.currentTimeMillis() + 500L, pending)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao agendar restart: ${e.message}")
        } finally {
            Process.killProcess(Process.myPid())
        }
    }

    private fun initStoneSDK() {
        if (!BuildConfig.STONE_ENABLED) {
            Log.d(TAG, "Stone SDK desabilitado neste flavor/build")
            return
        }
        try {
            Stone.setAppName("BliqTotem")
            val qrcodeAuth = BuildConfig.STONE_QRCODE_AUTHORIZATION
            val stoneKeys = mapOf(
                StoneKeyType.QRCODE_PROVIDERID   to BuildConfig.STONE_QRCODE_PROVIDER_ID,
                StoneKeyType.QRCODE_AUTHORIZATION to qrcodeAuth,
            )
            Log.d(TAG, "StoneStart.init — providerId=${BuildConfig.STONE_QRCODE_PROVIDER_ID} authToken=${qrcodeAuth.take(8)}…")
            StoneStart.init(this, stoneKeys)
            if (BuildConfig.STONE_CODE.isNotEmpty()) {
                activateStoneCode(BuildConfig.STONE_CODE)
            } else {
                startPeriodicReversal()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar Stone SDK: ${e.message}")
        }
    }

    private fun activateStoneCode(stoneCode: String) {
        stonePaymentManager.activate(this, stoneCode) { result ->
            result.onSuccess {
                Log.d(TAG, "Stone code ativado: $stoneCode")
                startPeriodicReversal()
            }.onFailure { e ->
                Log.e(TAG, "Falha ao ativar Stone code: ${e.message}")
            }
        }
    }

    private fun startPeriodicReversal() {
        appScope.launch {
            while (true) {
                stonePaymentManager.runReversal(this@BliqTotemApp)
                delay(REVERSAL_INTERVAL_MS)
            }
        }
    }

    companion object {
        private val SAIDAS_ANORMAIS = setOf("ANR", "CRASH", "CRASH_NATIVE", "LOW_MEMORY", "EXCESSIVE_RESOURCE_USAGE", "INITIALIZATION_FAILURE")

        private fun motivoSaida(reason: Int): String = when (reason) {
            ApplicationExitInfo.REASON_EXIT_SELF                -> "EXIT_SELF"
            ApplicationExitInfo.REASON_SIGNALED                 -> "SIGNALED"
            ApplicationExitInfo.REASON_LOW_MEMORY               -> "LOW_MEMORY"
            ApplicationExitInfo.REASON_CRASH                    -> "CRASH"
            ApplicationExitInfo.REASON_CRASH_NATIVE             -> "CRASH_NATIVE"
            ApplicationExitInfo.REASON_ANR                      -> "ANR"
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE   -> "INITIALIZATION_FAILURE"
            ApplicationExitInfo.REASON_PERMISSION_CHANGE        -> "PERMISSION_CHANGE"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
            ApplicationExitInfo.REASON_USER_REQUESTED           -> "USER_REQUESTED"
            ApplicationExitInfo.REASON_USER_STOPPED             -> "USER_STOPPED"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED          -> "DEPENDENCY_DIED"
            ApplicationExitInfo.REASON_OTHER                    -> "OTHER"
            else                                                -> "UNKNOWN($reason)"
        }

        private const val TAG = "StoneSDK"
        private const val REVERSAL_INTERVAL_MS = 60 * 60 * 1000L // 1 hora
    }
}
