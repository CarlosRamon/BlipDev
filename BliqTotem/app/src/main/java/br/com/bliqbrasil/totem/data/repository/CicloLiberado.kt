package br.com.bliqbrasil.totem.data.repository

import br.com.bliqbrasil.totem.data.model.CicloAtivo
import kotlinx.coroutines.delay

// Totens de uma unidade costumam sair pelo mesmo IP e o backend limita
// 100 req/min por IP: 15s mantém folga mesmo com vários boxes.
const val INTERVALO_CICLO_LIBERADO_MS = 15_000L

/**
 * Espera até o backend ter um ciclo ativo para este terminal e o devolve.
 *
 * Brinde e manutenção são liberados pelo painel do franqueado e chegam ao totem
 * só como um ciclo AGUARDANDO — não há push. As telas em que o cliente ainda não
 * comprou nada (Welcome e Identificação) chamam isto enquanto estão visíveis para
 * abrir a sessão sem que alguém precise tocar no totem.
 *
 * [podeVerificar] permite pular uma rodada, por exemplo enquanto um cadastro está
 * sendo gravado. Cancelar a coroutine encerra a espera.
 */
suspend fun PosRepository.aguardarCicloLiberado(
    podeVerificar: () -> Boolean = { true },
): CicloAtivo {
    while (true) {
        delay(INTERVALO_CICLO_LIBERADO_MS)
        if (!podeVerificar()) continue
        val ciclo = getCicloAtivo().getOrNull() ?: continue
        if (ciclo.segundosRestantes > 0) return ciclo
    }
}

/** Ciclo já iniciado retoma de onde parou; AGUARDANDO começa do zero. */
val CicloAtivo.retomarDeSegundos: Int?
    get() = if (status == "EM_ANDAMENTO") segundosRestantes else null
