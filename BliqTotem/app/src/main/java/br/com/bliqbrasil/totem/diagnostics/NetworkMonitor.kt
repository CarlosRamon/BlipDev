package br.com.bliqbrasil.totem.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock

/**
 * Acompanha a conexão do totem: registra quando a rede cai, quando volta (e
 * quanto tempo ficou fora) e quando o Wi-Fi está conectado mas sem internet —
 * roteador ligado com o link da operadora fora.
 */
class NetworkMonitor(context: Context, private val diagnostico: DiagnosticLogger) {

    data class Info(val tipo: String, val sinal: Int?)

    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var perdidaDesde: Long? = null
    private var semInternetDesde: Long? = null
    private var ultimoTipo = "NONE"

    fun iniciar() {
        ultimoTipo = info().tipo
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val atual = info()
                perdidaDesde?.let { desde ->
                    diagnostico.log(
                        Categoria.REDE, "REDE_RESTABELECIDA", Severidade.INFO,
                        "Rede ${atual.tipo} de volta",
                        mapOf("foraDoArSegundos" to segundosDesde(desde), "tipo" to atual.tipo, "sinal" to atual.sinal),
                    )
                }
                perdidaDesde = null
                ultimoTipo = atual.tipo
                diagnostico.enviarAgora()
            }

            override fun onLost(network: Network) {
                if (perdidaDesde != null) return
                perdidaDesde = SystemClock.elapsedRealtime()
                diagnostico.log(
                    Categoria.REDE, "REDE_PERDIDA", Severidade.AVISO,
                    "Rede $ultimoTipo caiu",
                    mapOf("tipo" to ultimoTipo),
                )
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val validada = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val desde = semInternetDesde
                if (!validada && desde == null) {
                    semInternetDesde = SystemClock.elapsedRealtime()
                    diagnostico.log(
                        Categoria.REDE, "REDE_SEM_INTERNET", Severidade.AVISO,
                        "Conectado à rede ${tipoDe(caps)}, mas sem acesso à internet",
                        mapOf("tipo" to tipoDe(caps)),
                        agruparPor = tipoDe(caps),
                    )
                } else if (validada && desde != null) {
                    semInternetDesde = null
                    diagnostico.log(
                        Categoria.REDE, "REDE_INTERNET_VOLTOU", Severidade.INFO,
                        "Internet de volta",
                        mapOf("semInternetSegundos" to segundosDesde(desde), "tipo" to tipoDe(caps)),
                    )
                    diagnostico.enviarAgora()
                }
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback)
            } else {
                cm.registerNetworkCallback(
                    NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                    callback,
                )
            }
        } catch (e: Exception) {
            diagnostico.log(Categoria.APP, "MONITOR_REDE_FALHOU", Severidade.AVISO, e.message)
        }
    }

    /** Tipo de rede ativa e, no Wi-Fi, o RSSI em dBm. */
    fun info(): Info {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return Info("DESCONHECIDO", null)
        val network = cm.activeNetwork ?: return Info("NONE", null)
        val caps = cm.getNetworkCapabilities(network) ?: return Info("NONE", null)
        val tipo = tipoDe(caps)
        val sinal = if (tipo == "WIFI") {
            @Suppress("DEPRECATION")
            (appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo.rssi
        } else null
        return Info(tipo, sinal)
    }

    private fun tipoDe(caps: NetworkCapabilities) = when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "WIFI"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
        else -> "NONE"
    }

    private fun segundosDesde(elapsed: Long) = (SystemClock.elapsedRealtime() - elapsed) / 1000
}
