package br.com.bliqbrasil.totem.data.network

import br.com.bliqbrasil.totem.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("/api/pos/ativar")
    suspend fun ativarPos(@Body request: AtivarRequest): ActivationResponse

    @GET("/api/pos/config")
    suspend fun getConfig(): PosConfig

    @GET("/api/pos/ciclos/ativo")
    suspend fun getCicloAtivo(): Response<CicloAtivo>

    @POST("/api/pos/ciclos")
    suspend fun criarCiclo(@Body request: CriarCicloRequest): Ciclo

    @PATCH("/api/pos/ciclos/{cicloId}/extra")
    suspend fun adicionarExtra(
        @Path("cicloId") cicloId: String,
        @Body request: AdicionarExtraRequest,
    ): Ciclo

    @PATCH("/api/pos/ciclos/{cicloId}/iniciar")
    suspend fun iniciarCiclo(@Path("cicloId") cicloId: String): Ciclo

    @PATCH("/api/pos/ciclos/{cicloId}/finalizar")
    suspend fun finalizarCiclo(@Path("cicloId") cicloId: String): Ciclo

    @GET("/api/pos/cliente/{cpf}")
    suspend fun buscarCliente(@Path("cpf") cpf: String): ClienteLookupResponse

    @POST("/api/pos/cliente")
    suspend fun criarCliente(@Body request: CriarClienteRequest): Cliente

    @POST("/api/pos/telemetria")
    suspend fun registrarTelemetria(@Body request: TelemetriaRequest): Response<Unit>

    @POST("/api/pos/heartbeat")
    suspend fun heartbeat(@Body request: HeartbeatRequest): HeartbeatResponse
}
