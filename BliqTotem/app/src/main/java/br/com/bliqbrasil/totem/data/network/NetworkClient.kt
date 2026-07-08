package br.com.bliqbrasil.totem.data.network

import br.com.bliqbrasil.totem.data.local.TokenStorage
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://app.bliqbrasil.com.br"

class NetworkClient(tokenStorage: TokenStorage) {

    val authInterceptor = AuthInterceptor(tokenStorage)

    private val okhttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okhttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

fun HttpException.apiMessage(): String {
    return try {
        val body = response()?.errorBody()?.string() ?: return "Erro HTTP ${code()}"
        Gson().fromJson(body, JsonObject::class.java)
            ?.get("error")?.asString ?: "Erro HTTP ${code()}"
    } catch (e: Exception) {
        "Erro HTTP ${code()}"
    }
}

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    Result.failure(Exception(e.apiMessage()))
} catch (e: Exception) {
    Result.failure(e)
}
