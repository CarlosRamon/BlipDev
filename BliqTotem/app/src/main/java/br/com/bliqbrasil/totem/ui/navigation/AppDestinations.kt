package br.com.bliqbrasil.totem.ui.navigation

import br.com.bliqbrasil.totem.data.model.PaymentMethod
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppDestinations {
    @Serializable object Activation
    @Serializable object Welcome
    @Serializable object CpfInput
    @Serializable object Support

    @Serializable
    data class Home(val clienteId: String = "")

    @Serializable
    data class MinutesPicker(
        val washOptionJson: String,
        val boxTipo: String = "LAVACAO",
        val clienteId: String = "",
    )

    @Serializable
    data class Extras(
        val washOptionJson: String,
        val boxTipo: String = "LAVACAO",
        val clienteId: String = "",
    )

    @Serializable
    data class Checkout(
        val washOptionJson: String,
        val selectedExtrasJson: String,
        val totalMinutes: Int,
        val totalPrice: Double,
        val boxTipo: String = "LAVACAO",
        val clienteId: String = "",
        // Compra nascida da oferta do fim do ciclo anterior; segue até o
        // criarCiclo para o backend marcar a origem.
        val crossSell: Boolean = false,
    )

    @Serializable
    data class Payment(
        val washOptionJson: String,
        val selectedExtrasJson: String,
        val totalMinutes: Int,
        val totalPrice: Double,
        val paymentMethod: String,
        val boxTipo: String = "LAVACAO",
        val clienteId: String = "",
        val crossSell: Boolean = false,
    )

    @Serializable
    data class Success(
        val paymentMethod: String,
        val totalMinutes: Int,
        val totalPrice: Double,
        val cicloId: String,
        val transacaoId: String,
        val boxTipo: String = "LAVACAO",
        // Repassados à sessão: o cliente para vincular a eventual recompra, e a
        // oferta a exibir quando o tempo acabar.
        val clienteId: String = "",
        val crossSellMinutos: Int? = null,
        val crossSellPreco: Double? = null,
    )

    @Serializable
    data class Session(
        val cicloId: String,
        val totalMinutes: Int,
        val boxTipo: String = "LAVACAO",
        val paymentMethod: String? = null,
        val totalPrice: Double? = null,
        val resumeFromSeconds: Int? = null,
        val clienteId: String = "",
        // Nulos quando o pacote não tem oferta configurada, quando a sessão é
        // uma retomada (o app reiniciou e não temos mais o produto em mãos) ou
        // quando esta própria sessão já veio de uma oferta aceita — ela é
        // oferecida uma vez só.
        val crossSellMinutos: Int? = null,
        val crossSellPreco: Double? = null,
    )
}

// Helpers to encode/decode complex nav args
fun WashOption.toJson(): String = Json.encodeToString(this)
fun String.toWashOption(): WashOption = Json.decodeFromString(this)

fun List<WashOptionExtra>.toJson(): String = Json.encodeToString(this)
fun String.toWashOptionExtras(): List<WashOptionExtra> = Json.decodeFromString(this)

fun PaymentMethod.toNavString() = name
fun String.toPaymentMethod() = PaymentMethod.valueOf(this)
