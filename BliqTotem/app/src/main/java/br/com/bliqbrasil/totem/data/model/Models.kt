package br.com.bliqbrasil.totem.data.model

import kotlinx.serialization.Serializable

// ── API response models ────────────────────────────────────────────────────

data class ActivationResponse(
    val token: String,
    val terminalId: String,
    val serial: String,
    val franqueadoId: String,
)

data class PosConfig(
    val terminal: Terminal,
    val franqueado: Franqueado,
    val box: Box,
    val esp32: Esp32Info,
    val produtos: List<Produto>,
)

data class Terminal(val id: String, val serial: String, val modelo: String?)
/**
 * Política de identificação do cliente, definida por franquia no painel.
 * O default OPCIONAL mantém o comportamento antigo caso o backend não envie
 * o campo (totem novo contra backend velho).
 */
enum class PoliticaCpf {
    DESATIVADO,   // pula a tela de identificação
    OPCIONAL,     // tela aparece, cliente pode pular
    OBRIGATORIO;  // tela aparece, sem opção de pular

    companion object {
        fun from(raw: String?): PoliticaCpf =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OPCIONAL
    }
}

data class Franqueado(
    val id: String,
    val nome: String,
    val status: String,
    val politicaCpf: String = "OPCIONAL",
)
data class Box(val id: String, val nome: String, val tipo: String = "LAVACAO", val status: String)
data class Esp32Info(
    val uuid: String? = null,           // campo legado (backend antigo)
    val serviceUuid: String? = null,
    val charUuid: String? = null,
    val versao: String?,
    val online: Boolean,
    val ultimaVezEm: String?,
)

data class Produto(
    val id: String,
    val nome: String,
    val tipo: String,
    val tempoMinutos: Int?,
    val preco: String,
    val boxId: String,
    val extras: List<ProdutoExtra>,
    // Oferta de minutos extras exibida quando ESTE pacote termina por tempo.
    // O que o cliente compra é minutagem avulsa, não um extra deste produto.
    val crossSellAtivo: Boolean = false,
    val crossSellMinutos: Int? = null,
    val crossSellPreco: String? = null,
)

data class ProdutoExtra(
    val id: String,
    val produtoId: String,
    val rotulo: String,
    val minutos: Int,
    val preco: String,
    val ativo: Boolean,
)

data class Ciclo(
    val id: String,
    val boxId: String,
    val terminalId: String,
    val produtoId: String,
    val tempoContratado: Int,
    val status: String,
    val createdAt: String,
)

data class CicloAtivo(
    val id: String,
    val tempoContratado: Int,
    val status: String,
    val segundosRestantes: Int,
)

// ── Termos de Uso ──────────────────────────────────────────────────────────

data class TermosAtual(
    val versao: String,
    val publicadoEm: String,
    val conteudo: String,
    val pdfUrl: String?,
)

// ── Cliente ────────────────────────────────────────────────────────────────

data class Cliente(
    val id: String,
    val cpf: String,
    val nome: String,
    val telefone: String,
    val email: String?,
)

data class ClienteLookupResponse(
    val encontrado: Boolean,
    val cliente: Cliente?,
    val nomePreenchido: String?,
)

// ── Cupons ─────────────────────────────────────────────────────────────────

/**
 * Cupom como o backend devolve na lista e na validação. `desconto` e
 * `valorFinal` vêm calculados pelo servidor quando o totem manda o valor de
 * tabela; o totem só exibe, não refaz a conta.
 */
data class CupomDisponivel(
    val codigo: String,
    val descricao: String?,
    val tipoDesconto: String,       // PERCENTUAL | VALOR_FIXO
    val valor: Double,
    val validoAte: String?,
    val desconto: Double? = null,
    val valorFinal: Double? = null,
)

/** ok=false traz `mensagem` pronta para o cliente (ex.: "Este cupom expirou"). */
data class ValidarCupomResponse(
    val ok: Boolean,
    val motivo: String? = null,
    val mensagem: String? = null,
    val codigo: String? = null,
    val descricao: String? = null,
    val tipoDesconto: String? = null,
    val valor: Double? = null,
    val desconto: Double? = null,
    val valorFinal: Double? = null,
)

data class ValidarCupomRequest(
    val codigo: String,
    val valorBruto: Double,
    val clienteId: String? = null,
    val crossSell: Boolean = false,
)

// ── API request models ─────────────────────────────────────────────────────

data class AtivarRequest(val codigo: String)

data class CriarCicloRequest(
    val produtoId: String,
    val tempoContratado: Int,
    val metodoPagamento: String,
    val valor: Double,
    val transacaoId: String? = null,
    val clienteId: String? = null,
    val crossSell: Boolean = false,
    // Com cupom: valor é o cobrado e valorBruto o de tabela (pacote + extras).
    val cupomCodigo: String? = null,
    val valorBruto: Double? = null,
)

data class CriarClienteRequest(
    val cpf: String,
    val nome: String,
    val telefone: String,
    val email: String? = null,
    val termosVersao: String,
    val aceitaMarketing: Boolean = false,
    val aceitaUsoImagem: Boolean = false,
)

data class AdicionarExtraRequest(val produtoExtraId: String)

/** Lote do diagnóstico — ver DiagnosticLogger. Eventos já vêm serializados da fila. */
data class DiagnosticoLote(
    val enviadoEm: String,
    val eventos: List<com.google.gson.JsonObject>,
)

data class TelemetriaRequest(
    val evento: String,
    val dados: Map<String, Any?>? = null,
)

data class HeartbeatRequest(
    val bleStatus: String,
    val networkType: String,
    val signalStrength: Int?,
    val appVersion: String,
)

data class HeartbeatResponse(
    val ciclo: CicloAtivo?,
)

// ── Internal / navigation models ───────────────────────────────────────────

@Serializable
data class WashOption(
    val id: String,
    val label: String,
    val minutes: Int,
    val price: Double,
    val tipo: String = "TEMPO_FIXO",
    val extras: List<WashOptionExtra>,
    // Oferta do fim do ciclo, quando o franqueado configurou uma para o pacote.
    // Viaja junto na navegação até a sessão, que é quem a exibe.
    val crossSellMinutos: Int? = null,
    val crossSellPreco: Double? = null,
)

@Serializable
data class WashOptionExtra(
    val id: String,
    val rotulo: String,
    val minutos: Int,
    val preco: Double,
)

/** CUPOM = total zerado por cupom: não passa pela Stone. */
enum class PaymentMethod { CREDIT, DEBIT, PIX, CUPOM }

enum class Machine(val label: String, val icon: String, val colorHex: Long) {
    PRE_LAVAGEM  ("Pré-lavagem",   "💦", 0xFF1A73E8),
    SHAMPOO      ("Shampoo",       "🫧", 0xFF9C27B0),
    ENXAGUE      ("Enxague",       "🚿", 0xFF00897B),
    ASPIRADOR    ("Aspirador",     "🌀", 0xFF5C6BC0),
    AR_COMPRIMIDO("Ar comprimido", "💨", 0xFF00ACC1),
}

fun machinesForTipo(tipo: String): List<Machine> = when (tipo) {
    "ASPIRACAO" -> listOf(Machine.ASPIRADOR, Machine.AR_COMPRIMIDO)
    else        -> listOf(Machine.PRE_LAVAGEM, Machine.SHAMPOO, Machine.ENXAGUE)
}

// ── BLE ────────────────────────────────────────────────────────────────────

enum class ConnectionStatus { DISCONNECTED, SCANNING, CONNECTING, CONNECTED, RECONNECTING, ERROR }

// ── Payment ────────────────────────────────────────────────────────────────

data class PaymentResult(
    val success: Boolean,
    val transactionId: String,
    val method: PaymentMethod,
    val amount: Double,
    val timestamp: Long,
)
