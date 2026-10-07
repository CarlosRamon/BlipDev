package br.com.bliqbrasil.totem.diagnostics

import android.os.SystemClock
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

private const val REQUISICAO_LENTA_MS = 8_000L
private const val ROTA_DIAGNOSTICO = "/api/pos/diagnostico"

private val UUID_RE = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
private val NUMERO_RE = Regex("/\\d{6,}")

/**
 * Agrupa a rota por formato: ids viram `:id` e números longos viram `:num`.
 * O CPF vai no path de /cliente/{cpf} e não pode chegar ao log.
 */
fun rotaAgrupada(path: String): String =
    path.replace(UUID_RE, ":id").replace(NUMERO_RE, "/:num")

/**
 * Registra no diagnóstico as chamadas à API que falharam: sem rede (DNS,
 * timeout, conexão recusada, TLS), erro 5xx, token recusado e respostas lentas.
 */
class DiagnosticInterceptor(private val diagnostico: () -> DiagnosticLogger) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val rota = rotaAgrupada(request.url.encodedPath)
        // O envio do diagnóstico não registra a si mesmo — falhar sem rede é esperado.
        if (rota == ROTA_DIAGNOSTICO) return chain.proceed(request)

        val inicio = SystemClock.elapsedRealtime()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // Cancelamento é o app saindo da tela, não falha de rede.
            if (!chain.call().isCanceled()) {
                val erro = e.javaClass.simpleName
                diagnostico().log(
                    Categoria.API, "API_FALHA_REDE", Severidade.AVISO,
                    "${request.method} $rota: $erro",
                    mapOf(
                        "metodo" to request.method,
                        "rota" to rota,
                        "erro" to erro,
                        "detalhe" to e.message,
                        "duracaoMs" to SystemClock.elapsedRealtime() - inicio,
                    ),
                    agruparPor = "$rota|$erro",
                )
            }
            throw e
        }

        val duracaoMs = SystemClock.elapsedRealtime() - inicio
        val dados = mapOf(
            "metodo" to request.method,
            "rota" to rota,
            "status" to response.code,
            "duracaoMs" to duracaoMs,
        )
        when {
            response.code >= 500 -> diagnostico().log(
                Categoria.API, "API_ERRO_SERVIDOR", Severidade.ERRO,
                "${request.method} $rota → ${response.code}", dados,
                agruparPor = "$rota|${response.code}",
            )
            response.code == 401 -> diagnostico().log(
                Categoria.API, "API_NAO_AUTORIZADO", Severidade.ERRO,
                "Token do terminal recusado em $rota", dados,
                agruparPor = rota,
            )
            duracaoMs > REQUISICAO_LENTA_MS -> diagnostico().log(
                Categoria.API, "API_LENTA", Severidade.AVISO,
                "${request.method} $rota levou ${duracaoMs}ms", dados,
                agruparPor = rota,
            )
        }
        return response
    }
}
