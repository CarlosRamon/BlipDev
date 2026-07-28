package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Paleta oficial Bliq (Projeto de Marca — Identidade Visual) ────────────────
// Extraída da apresentação de marca:
//   Azul Bliq   #1679ED  — primária (idêntica ao logo)
//   Deep Navy   #001B2D  — texto forte, superfícies escuras
//   Off-white   #EDEDED  — fundo institucional

val BliqBlue      = Color(0xFF1679ED)
val BliqBlueDeep  = Color(0xFF0D5CB6)
val BliqBlueSoft  = Color(0xFFE8F1FE)
val BliqNavy      = Color(0xFF001B2D)
val BliqNavySoft  = Color(0xFF1F3448)
val BliqOffWhite  = Color(0xFFEDEDED)

// ── Tokens semânticos (usados pelo MaterialTheme e por toda a UI) ──────────────

val Primary      = BliqBlue
val PrimaryDeep  = BliqBlueDeep
val PrimaryLight = BliqBlueSoft
val Background   = BliqOffWhite
val Surface      = Color(0xFFFFFFFF)
val OnSurface    = BliqNavy
val Secondary    = Color(0xFF4A5B6E)
val Tertiary     = Color(0xFF8695A6)
val Divider      = Color(0xFFE1E5EA)

// ── Cores de estado ────────────────────────────────────────────────────────────

val Success      = Color(0xFF1FA96A)
val SuccessLight = Color(0xFFE1F4EB)
val Warning      = Color(0xFFF5A524)
val WarningLight = Color(0xFFFDF0DC)
val Error        = Color(0xFFE0364B)
val ErrorSurface = Color(0xFFFCE5E9)

// ── Cores de máquina / categorias auxiliares ──────────────────────────────────

val Purple       = Color(0xFF7A3EE8)
val Teal         = Color(0xFF00A79D)

// ── Gradientes de marca ────────────────────────────────────────────────────────
// Usar via Modifier.background(BrandGradientHero) em headers/CTAs.

val BrandGradientHero: Brush = Brush.linearGradient(
    colors = listOf(BliqBlue, BliqBlueDeep),
)

val BrandGradientDeep: Brush = Brush.linearGradient(
    colors = listOf(BliqBlueDeep, BliqNavy),
)

val BrandGradientSoft: Brush = Brush.linearGradient(
    colors = listOf(BliqBlueSoft, Surface),
)
