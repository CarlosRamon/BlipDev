package br.com.bliqbrasil.totem.diagnostics

import android.content.Context
import android.os.SystemClock
import android.util.Log
import br.com.bliqbrasil.totem.BuildConfig
import br.com.bliqbrasil.totem.data.model.DiagnosticoLote
import br.com.bliqbrasil.totem.data.network.ApiService
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

enum class Categoria { BLE, REDE, API, APP, PAGAMENTO }
enum class Severidade { INFO, AVISO, ERRO }

private const val TAG = "Diagnostico"

private const val INTERVALO_ENVIO_MS = 60_000L
private const val EVENTOS_POR_LOTE = 100
// Uma rodada esvazia até isto; o resto vai na próxima, para não monopolizar a rede.
private const val LOTES_POR_RODADA = 10
// ~1 MB em disco. Acima disso o totem está offline há dias e os mais antigos saem.
private const val MAX_EVENTOS_NA_FILA = 5_000
private const val JANELA_AGRUPAMENTO_MS = 5 * 60_000L
private const val MAX_MENSAGEM = 500
private const val MAX_DADOS = 3_500

/**
 * Registro de problemas do totem para o backend (/api/pos/diagnostico).
 *
 * Os eventos vão primeiro para um arquivo e só depois para a rede: o que mais
 * interessa registrar — queda de internet, app reiniciando — é justamente o que
 * impede o envio na hora. A fila sobrevive a reinícios e é esvaziada em lotes
 * quando há conexão; o id de cada evento é gerado aqui, então um lote reenviado
 * não duplica nada no servidor.
 *
 * Erros que se repetem (heartbeat falhando a cada 30s sem rede) são agrupados:
 * com [agruparPor], o mesmo problema só é gravado uma vez a cada 5 minutos e o
 * próximo registro leva quantas repetições foram omitidas.
 */
class DiagnosticLogger(
    context: Context,
    private val api: () -> ApiService,
    private val temToken: () -> Boolean,
) {
    /** Identifica esta execução do app: eventos com o mesmo bootId vieram do mesmo processo. */
    val bootId: String = UUID.randomUUID().toString()

    /** Ciclo em andamento — marcado pela sessão para separar falhas que atingiram cliente. */
    @Volatile var cicloId: String? = null

    /** Estado do totem anexado a todo evento (BLE, rede). Definido pelo BliqTotemApp. */
    @Volatile var contexto: () -> Map<String, Any?> = { emptyMap() }

    private val fila = File(File(context.filesDir, "diagnostico").apply { mkdirs() }, "fila.jsonl")
    private val lock = Any()
    private val gson = Gson()
    private val escritor = Executors.newSingleThreadExecutor { r -> Thread(r, "Diagnostico").apply { isDaemon = true } }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val envioMutex = Mutex()

    private class Agrupamento(val desde: Long, var omitidos: Int = 0)
    private val agrupamentos = ConcurrentHashMap<String, Agrupamento>()

    private var eventosNaFila = synchronized(lock) { contarLinhas() }

    fun iniciar() {
        scope.launch {
            while (true) {
                delay(INTERVALO_ENVIO_MS)
                enviar()
            }
        }
    }

    /** Antecipa o envio — quando a rede volta não faz sentido esperar o próximo ciclo. */
    fun enviarAgora() {
        scope.launch { enviar() }
    }

    fun log(
        categoria: Categoria,
        tipo: String,
        severidade: Severidade,
        mensagem: String? = null,
        dados: Map<String, Any?> = emptyMap(),
        agruparPor: String? = null,
    ) {
        val evento = montar(categoria, tipo, severidade, mensagem, dados, agruparPor) ?: return
        Log.println(if (severidade == Severidade.INFO) Log.INFO else Log.WARN, TAG, "$tipo ${mensagem.orEmpty()} $dados")
        escritor.execute { gravar(evento) }
    }

    /**
     * Grava na mesma thread. Só para o handler de crash: o processo morre logo
     * em seguida e o executor não chegaria a rodar.
     */
    fun logImediato(
        categoria: Categoria,
        tipo: String,
        severidade: Severidade,
        mensagem: String? = null,
        dados: Map<String, Any?> = emptyMap(),
    ) {
        val evento = montar(categoria, tipo, severidade, mensagem, dados, null) ?: return
        gravar(evento)
    }

    // ── Montagem ──────────────────────────────────────────────────────────

    private fun montar(
        categoria: Categoria,
        tipo: String,
        severidade: Severidade,
        mensagem: String?,
        dados: Map<String, Any?>,
        agruparPor: String?,
    ): String? {
        val extras = mutableMapOf<String, Any?>()
        if (agruparPor != null) {
            val chave = "$tipo|$agruparPor"
            val agora = SystemClock.elapsedRealtime()
            val anterior = agrupamentos[chave]
            if (anterior != null && agora - anterior.desde < JANELA_AGRUPAMENTO_MS) {
                anterior.omitidos++
                return null
            }
            if (anterior != null && anterior.omitidos > 0) extras["repeticoesOmitidas"] = anterior.omitidos
            agrupamentos[chave] = Agrupamento(agora)
        }

        val ctx = try { contexto() } catch (_: Exception) { emptyMap() }
        val todosDados = (ctx + dados + extras).filterValues { it != null }
        var dadosJson = gson.toJsonTree(todosDados).asJsonObject
        if (dadosJson.toString().length > MAX_DADOS) {
            dadosJson = JsonObject().apply { addProperty("truncado", dadosJson.toString().take(MAX_DADOS - 100)) }
        }

        return JsonObject().apply {
            addProperty("id", UUID.randomUUID().toString())
            addProperty("categoria", categoria.name)
            addProperty("tipo", tipo)
            addProperty("severidade", severidade.name)
            mensagem?.let { addProperty("mensagem", it.take(MAX_MENSAGEM)) }
            if (dadosJson.size() > 0) add("dados", dadosJson)
            cicloId?.let { addProperty("cicloId", it) }
            addProperty("appVersion", BuildConfig.VERSION_NAME)
            addProperty("bootId", bootId)
            addProperty("ocorridoEm", agoraIso())
        }.toString()
    }

    // ── Fila em disco ─────────────────────────────────────────────────────

    private fun gravar(linha: String) {
        synchronized(lock) {
            try {
                if (eventosNaFila >= MAX_EVENTOS_NA_FILA) descartarMaisAntigos()
                fila.appendText(linha + "\n")
                eventosNaFila++
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao gravar evento de diagnóstico: ${e.message}")
            }
        }
    }

    // Chamado com o lock. Mantém 80% da fila e registra quanto se perdeu.
    private fun descartarMaisAntigos() {
        val linhas = fila.readLines()
        val manter = linhas.takeLast(MAX_EVENTOS_NA_FILA * 8 / 10)
        val descartados = linhas.size - manter.size
        reescrever(manter)
        montar(
            Categoria.APP, "FILA_DIAGNOSTICO_CHEIA", Severidade.AVISO,
            "Fila de diagnóstico cheia — $descartados eventos antigos descartados",
            mapOf("descartados" to descartados), null,
        )?.let { fila.appendText(it + "\n"); eventosNaFila++ }
    }

    private fun reescrever(linhas: List<String>) {
        val tmp = File(fila.parentFile, "fila.tmp")
        tmp.writeText(if (linhas.isEmpty()) "" else linhas.joinToString("\n", postfix = "\n"))
        tmp.renameTo(fila)
        eventosNaFila = linhas.size
    }

    private fun contarLinhas(): Int =
        if (fila.exists()) fila.useLines { seq -> seq.count { it.isNotBlank() } } else 0

    // ── Envio ─────────────────────────────────────────────────────────────

    private suspend fun enviar() = envioMutex.withLock {
        if (!temToken()) return@withLock
        repeat(LOTES_POR_RODADA) {
            val lote = synchronized(lock) {
                if (!fila.exists()) emptyList() else fila.useLines { it.filter(String::isNotBlank).take(EVENTOS_POR_LOTE).toList() }
            }
            if (lote.isEmpty()) return@withLock

            val eventos = lote.mapNotNull { runCatching { gson.fromJson(it, JsonObject::class.java) }.getOrNull() }
            if (eventos.isEmpty()) {
                removerPrimeiros(lote.size)
                return@repeat
            }

            val resposta = try {
                api().enviarDiagnostico(DiagnosticoLote(enviadoEm = agoraIso(), eventos = eventos))
            } catch (_: Exception) {
                return@withLock // sem rede: fica na fila para a próxima rodada
            }
            when {
                resposta.isSuccessful -> removerPrimeiros(lote.size)
                // Lote que o servidor nunca vai aceitar: descartar, senão trava a fila.
                resposta.code() == 400 || resposta.code() == 413 || resposta.code() == 422 -> {
                    Log.w(TAG, "Lote de diagnóstico recusado (${resposta.code()}) — ${lote.size} eventos descartados")
                    removerPrimeiros(lote.size)
                }
                // 404 (backend ainda sem o endpoint), 401, 429, 5xx: guarda e tenta depois.
                else -> return@withLock
            }
        }
    }

    private fun removerPrimeiros(n: Int) {
        synchronized(lock) {
            // Novos eventos só entram no fim do arquivo: os n primeiros são os enviados.
            val linhas = fila.readLines().filter(String::isNotBlank)
            reescrever(linhas.drop(n))
        }
    }

    companion object {
        fun agoraIso(): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date())
    }
}
