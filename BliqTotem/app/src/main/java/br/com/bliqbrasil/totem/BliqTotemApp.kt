package br.com.bliqbrasil.totem

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import br.com.bliqbrasil.totem.bluetooth.BliqBleManager
import br.com.bliqbrasil.totem.data.local.TokenStorage
import br.com.bliqbrasil.totem.data.network.NetworkClient
import br.com.bliqbrasil.totem.data.repository.PosRepository
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

    val networkClient: NetworkClient by lazy { NetworkClient(tokenStorage) }

    val repository: PosRepository by lazy { PosRepository(networkClient.api) }

    val bleManager: BliqBleManager by lazy { BliqBleManager(this) }

    val stonePaymentManager: StonePaymentManager by lazy { StonePaymentManager() }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        installThirdPartyCrashFilter()
        initStoneSDK()
    }

    // Absorve NPEs do SDK da Stone; para crashes do nosso código, reinicia o app automaticamente.
    // Totem de autoatendimento — usuários não sabem agir diante de tela de erro.
    private fun installThirdPartyCrashFilter() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val origin = throwable.stackTrace.firstOrNull()?.className ?: ""
            val isThirdParty = origin.startsWith("stone.") ||
                origin.startsWith("br.com.stone.") ||
                origin.startsWith("br.com.inovare.")
            if (isThirdParty) {
                Log.e(TAG, "Exceção de SDK de terceiros absorvida [${thread.name}]: ${throwable.message}", throwable)
            } else {
                Log.e(TAG, "Crash da aplicação — reiniciando [${thread.name}]: ${throwable.message}", throwable)
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
        private const val TAG = "StoneSDK"
        private const val REVERSAL_INTERVAL_MS = 60 * 60 * 1000L // 1 hora
    }
}
