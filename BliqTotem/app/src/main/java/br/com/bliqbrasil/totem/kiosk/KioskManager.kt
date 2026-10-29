package br.com.bliqbrasil.totem.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import br.com.bliqbrasil.totem.diagnostics.Categoria
import br.com.bliqbrasil.totem.diagnostics.DiagnosticLogger
import br.com.bliqbrasil.totem.diagnostics.Severidade

private const val TAG = "Quiosque"
private const val PREFS = "bliq_quiosque"
private const val KEY_ATIVO = "ativo"

/** Senha do operador para ligar/desligar o bloqueio na tela inicial. */
private const val SENHA = "1020"

/**
 * Pacotes que podem abrir telas próprias com o totem travado. O Lock Task do
 * Android bloqueia qualquer activity de outro app que não esteja na lista — e
 * a Stone e o fabricante do POS abrem telas de PIN/pagamento por conta própria.
 */
private val PREFIXOS_LIBERADOS = listOf(
    "br.com.stone.", "com.pos.", "com.xcheng.", "com.tectoy.", "com.sunmi.", "woyou.",
    "com.ingenico.", "com.usdk.", "br.com.positivo.", "com.positivo.", "br.com.gertec.", "com.pax.",
)

// Mesmo liberados pelo prefixo, estes dariam ao cliente uma saída do totem.
private val PACOTES_BLOQUEADOS = setOf(
    "br.com.stone.posandroid.launcherapp",
    "br.com.stone.posandroid.settingsapp",
)

/**
 * Modo quiosque: trava o totem no app, sem Home nem Recentes.
 *
 * Usa o Lock Task Mode do Android (API 21+, vale para o Android 7.1 dos POS
 * mais antigos). Há dois níveis, conforme o terminal foi preparado:
 *
 * - **Device owner** (configurado uma vez via adb, ver [KioskAdminReceiver]):
 *   trava sem pedir confirmação, não há gesto de saída e o app vira a tela
 *   inicial do aparelho — volta sozinho depois de um reboot.
 * - **Sem device owner**: cai na fixação de tela comum. O Android pede
 *   confirmação ao ligar, e segurar Voltar + Início/Recentes destrava.
 *
 * A escolha de ficar travado é persistida: a MainActivity reaplica em todo
 * onResume, inclusive após reiniciar o app ou o aparelho.
 */
class KioskManager(
    private val context: Context,
    private val diagnostico: DiagnosticLogger,
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(context, KioskAdminReceiver::class.java)
    // Alias da Splash com categoria HOME, desligado no manifest. Só é ligado
    // como device owner — ligado sem isso, o Android pergunta qual launcher usar.
    private val home = ComponentName(context.packageName, "${context.packageName}.KioskHome")

    val ativo: Boolean get() = prefs.getBoolean(KEY_ATIVO, false)

    /** Device owner: bloqueio completo. Sem isso, é só fixação de tela. */
    val bloqueioCompleto: Boolean get() = dpm.isDeviceOwnerApp(context.packageName)

    fun senhaConfere(senha: String) = senha == SENHA

    fun ativar(activity: Activity) {
        prefs.edit().putBoolean(KEY_ATIVO, true).apply()
        diagnostico.log(Categoria.APP, "QUIOSQUE_ATIVADO", Severidade.INFO, "Botões do sistema bloqueados",
            mapOf("deviceOwner" to bloqueioCompleto))
        aplicar(activity)
    }

    fun desativar(activity: Activity) {
        prefs.edit().putBoolean(KEY_ATIVO, false).apply()
        diagnostico.log(Categoria.APP, "QUIOSQUE_DESATIVADO", Severidade.INFO, "Botões do sistema liberados",
            mapOf("deviceOwner" to bloqueioCompleto))
        if (bloqueioCompleto) {
            runCatching { dpm.clearPackagePersistentPreferredActivities(admin, context.packageName) }
            definirHome(false)
        }
        if (emLockTask()) runCatching { activity.stopLockTask() }
            .onFailure { Log.w(TAG, "stopLockTask falhou", it) }
    }

    /** Reaplica o bloqueio se ele estiver ligado. Chamado no onResume da MainActivity. */
    fun aplicar(activity: Activity) {
        if (!ativo) return
        if (bloqueioCompleto) configurarDeviceOwner()
        if (!emLockTask()) runCatching { activity.startLockTask() }
            .onFailure { Log.w(TAG, "startLockTask falhou", it) }
    }

    private fun configurarDeviceOwner() = runCatching {
        dpm.setLockTaskPackages(admin, pacotesLiberados())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Só o menu do botão de energia continua: o operador precisa conseguir reiniciar.
            dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS)
        }
        definirHome(true)
        val filtro = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(admin, filtro, home)
    }.onFailure { Log.w(TAG, "Falha ao configurar device owner", it) }

    private fun pacotesLiberados(): Array<String> {
        val instalados = context.packageManager.getInstalledPackages(0)
            .map { it.packageName }
            .filter { pkg -> PREFIXOS_LIBERADOS.any(pkg::startsWith) && pkg !in PACOTES_BLOQUEADOS }
        return (listOf(context.packageName) + instalados).distinct().toTypedArray()
    }

    private fun definirHome(ligado: Boolean) {
        val estado = if (ligado) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                     else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(home, estado, PackageManager.DONT_KILL_APP)
    }

    private fun emLockTask(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } else {
            @Suppress("DEPRECATION")
            am.isInLockTaskMode
        }
    }
}
