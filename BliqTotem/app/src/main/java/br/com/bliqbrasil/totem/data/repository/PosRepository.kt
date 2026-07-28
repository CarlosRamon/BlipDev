package br.com.bliqbrasil.totem.data.repository

import br.com.bliqbrasil.totem.data.model.*
import br.com.bliqbrasil.totem.data.network.ApiService
import br.com.bliqbrasil.totem.data.network.apiMessage
import br.com.bliqbrasil.totem.data.network.safeApiCall
import kotlinx.coroutines.delay
import retrofit2.HttpException

class CicloConflictException(val ativo: CicloAtivo?) : Exception("Sessão já ativa")

class PosRepository(private val api: ApiService) {

    suspend fun ativarPos(codigo: String): Result<ActivationResponse> =
        safeApiCall { api.ativarPos(AtivarRequest(codigo)) }

    suspend fun getConfig(): Result<PosConfig> =
        safeApiCall { api.getConfig() }

    suspend fun getCicloAtivo(): Result<CicloAtivo?> = safeApiCall {
        val response = api.getCicloAtivo()
        if (response.code() == 204 || !response.isSuccessful) null else response.body()
    }

    suspend fun getTermosAtual(): Result<TermosAtual> =
        safeApiCall { api.getTermosAtual() }

    suspend fun buscarCliente(cpf: String): Result<ClienteLookupResponse> =
        safeApiCall { api.buscarCliente(cpf) }

    suspend fun criarCliente(
        cpf: String,
        nome: String,
        telefone: String,
        termosVersao: String,
        aceitaMarketing: Boolean,
    ): Result<Cliente> = safeApiCall {
        api.criarCliente(
            CriarClienteRequest(
                cpf = cpf,
                nome = nome,
                telefone = telefone,
                termosVersao = termosVersao,
                aceitaMarketing = aceitaMarketing,
            )
        )
    }

    suspend fun criarCiclo(
        produtoId: String,
        tempoContratado: Int,
        metodoPagamento: String,
        valor: Double,
        transacaoId: String?,
        clienteId: String? = null,
    ): Result<Ciclo> = try {
        Result.success(
            api.criarCiclo(CriarCicloRequest(produtoId, tempoContratado, metodoPagamento, valor, transacaoId, clienteId))
        )
    } catch (e: HttpException) {
        if (e.code() == 409) {
            val ativo = try {
                val resp = api.getCicloAtivo()
                val body = if (resp.isSuccessful) resp.body() else null
                if (body != null && body.segundosRestantes > 0) body else null
            } catch (_: Exception) { null }
            Result.failure(CicloConflictException(ativo))
        } else {
            Result.failure(Exception(e.apiMessage()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun adicionarExtra(cicloId: String, produtoExtraId: String): Result<Ciclo> =
        safeApiCall { api.adicionarExtra(cicloId, AdicionarExtraRequest(produtoExtraId)) }

    suspend fun iniciarCiclo(cicloId: String): Result<Ciclo> =
        safeApiCall { api.iniciarCiclo(cicloId) }

    suspend fun finalizarCicloComRetry(cicloId: String): Boolean {
        repeat(4) { attempt ->
            try {
                api.finalizarCiclo(cicloId)
                return true
            } catch (_: Exception) {
                if (attempt < 3) delay(1500L * (attempt + 1))
            }
        }
        return false
    }

    fun registrarTelemetria(evento: String, dados: Map<String, Any?> = emptyMap()) {
        // Fire-and-forget — chamado via viewModelScope.launch
    }

    suspend fun registrarTelemetriaInternal(evento: String, dados: Map<String, Any?>) {
        try { api.registrarTelemetria(TelemetriaRequest(evento, dados)) } catch (_: Exception) {}
    }

    suspend fun heartbeat(
        bleStatus: String,
        networkType: String,
        signalStrength: Int?,
        appVersion: String,
    ): Result<HeartbeatResponse> = safeApiCall {
        api.heartbeat(HeartbeatRequest(bleStatus, networkType, signalStrength, appVersion))
    }

    suspend fun processPayment(method: PaymentMethod, amount: Double): PaymentResult {
        delay(2_000)
        val txId = (1..8).map { ('A'..'Z').random() }.joinToString("")
        return PaymentResult(
            success = true,
            transactionId = txId,
            method = method,
            amount = amount,
            timestamp = System.currentTimeMillis(),
        )
    }
}
