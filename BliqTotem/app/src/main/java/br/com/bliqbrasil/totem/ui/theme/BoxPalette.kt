package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tipo do box que o totem controla. Determina toda a identidade cromática do app.
 */
enum class BoxTipo {
    LAVACAO,
    ASPIRACAO;

    companion object {
        const val RAW_LAVACAO = "LAVACAO"
        const val RAW_ASPIRACAO = "ASPIRACAO"

        fun from(raw: String?): BoxTipo =
            if (raw?.uppercase() == RAW_ASPIRACAO) ASPIRACAO else LAVACAO
    }
}

/**
 * Paleta dependente do tipo de box.
 *
 * O sistema do Figma é uma **inversão de dois papéis**: `background` e `contrast`
 * trocam de lugar entre Lavação e Aspiração. Na Lavação o fundo da tela é o azul
 * Bliq e a topbar/cards de destaque são navy; na Aspiração é o oposto.
 *
 * O que NÃO inverte: superfícies de card (sempre brancas), o chip da máquina ativa
 * (sempre azul suave) e o `accent` de títulos e preços sobre card branco (sempre
 * azul) — esses são fixos nos dois tipos.
 */
@Immutable
data class BoxPalette(
    val tipo: BoxTipo,
    /** Fundo da tela. Azul na Lavação, navy na Aspiração. */
    val background: Color,
    val onBackground: Color,
    /** Cor oposta ao fundo: topbar, card selecionado, card de destaque, timer. */
    val contrast: Color,
    val onContrast: Color,
    /** Superfície de card — branca nos dois tipos. */
    val surface: Color,
    val onSurface: Color,
    /** Títulos e preços sobre card branco — azul nos dois tipos. */
    val accent: Color,
    /** Pastilha de ícone de item não selecionado. */
    val iconChip: Color,
    val onIconChip: Color,
    /** Chip da máquina ativa — azul suave nos dois tipos. */
    val chipSoft: Color,
    val onChipSoft: Color,
    /** CTA principal sobre o fundo da tela: branco na Lavação, azul na Aspiração. */
    val ctaContainer: Color,
    val onCtaContainer: Color,
    /** Tint do logo sobre o fundo da tela. */
    val logoTint: Color,
    /** Ação destrutiva sobre o fundo da tela — precisa contrastar com ele, não com branco. */
    val danger: Color,
    /**
     * Fundo alternativo, usado nas telas de confirmação (Success). Na Lavação é
     * branco em vez do azul; na Aspiração continua navy. Não é derivável de
     * [background] — vem assim dos mockups.
     */
    val backgroundAlt: Color,
    val onBackgroundAlt: Color,
)

val LavacaoPalette = BoxPalette(
    tipo         = BoxTipo.LAVACAO,
    background   = BliqBlue,
    onBackground = Surface,
    contrast     = BliqNavy,
    onContrast   = Surface,
    surface      = Surface,
    onSurface    = BliqNavy,
    accent       = BliqBlue,
    iconChip     = BliqBlue,
    onIconChip   = Surface,
    chipSoft     = BliqBlueSoft,
    onChipSoft   = BliqBlue,
    ctaContainer   = Surface,
    onCtaContainer = BliqNavy,
    logoTint       = Surface,
    danger         = Color(0xFFFFDDDD),
    backgroundAlt   = Surface,
    onBackgroundAlt = BliqNavy,
)

val AspiracaoPalette = BoxPalette(
    tipo         = BoxTipo.ASPIRACAO,
    background   = BliqNavy,
    onBackground = Surface,
    contrast     = BliqBlue,
    onContrast   = Surface,
    surface      = Surface,
    onSurface    = BliqNavy,
    accent       = BliqBlue,
    iconChip     = BliqBlueSoft,
    onIconChip   = BliqBlue,
    chipSoft     = BliqBlueSoft,
    onChipSoft   = BliqBlue,
    ctaContainer   = BliqBlue,
    onCtaContainer = Surface,
    logoTint       = BliqBlue,
    danger         = Color(0xFFFF4D4D),
    backgroundAlt   = BliqNavy,
    onBackgroundAlt = Surface,
)

fun paletteFor(tipo: BoxTipo): BoxPalette =
    when (tipo) {
        BoxTipo.LAVACAO   -> LavacaoPalette
        BoxTipo.ASPIRACAO -> AspiracaoPalette
    }

val LocalBoxPalette = staticCompositionLocalOf { LavacaoPalette }
