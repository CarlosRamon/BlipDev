package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Paleta oficial Bliq ───────────────────────────────────────────────────────
// Valores extraídos por amostragem de pixel dos mockups do Figma
// ("Figma Bliq Totem"), que são a fonte da verdade do layout:
//   Azul Bliq  #307FE2 — primária
//   Deep Navy  #051C2C — superfícies escuras, texto forte
//   Azul suave #EBF3FC — pastilhas e chips sobre superfície branca

val BliqBlue      = Color(0xFF307FE2)
val BliqBlueDeep  = Color(0xFF1F63BE)
val BliqBlueSoft  = Color(0xFFEBF3FC)
val BliqNavy      = Color(0xFF051C2C)
val BliqNavySoft  = Color(0xFF1F3448)
val BliqOffWhite  = Color(0xFFEDEDED)

// ── Tokens semânticos fixos (não variam por tipo de box) ──────────────────────

val Primary      = BliqBlue
val PrimaryDeep  = BliqBlueDeep
val PrimaryLight = BliqBlueSoft
val Background   = BliqOffWhite
val Surface      = Color(0xFFFFFFFF)
val OnSurface    = BliqNavy
val Secondary    = Color(0xFF4A5B6E)
val Tertiary     = Color(0xFF8695A6)
val Divider      = Color(0xFFE2E5EB)

// ── Cores de estado ───────────────────────────────────────────────────────────

// Verde da marca, amostrado dos mockups: é um teal, não o verde puro anterior.
val Success      = Color(0xFF2EC4B6)
val SuccessLight = Color(0xFFDCF3F0)
val Warning      = Color(0xFFF5A524)
val WarningLight = Color(0xFFFDF0DC)
val Error        = Color(0xFFE0364B)
val ErrorSurface = Color(0xFFFCE5E9)

// ── Gradientes de marca ───────────────────────────────────────────────────────

val BrandGradientHero: Brush = Brush.linearGradient(
    colors = listOf(BliqBlue, BliqBlueDeep),
)

val BrandGradientDeep: Brush = Brush.linearGradient(
    colors = listOf(BliqBlueDeep, BliqNavy),
)

val BrandGradientSoft: Brush = Brush.linearGradient(
    colors = listOf(BliqBlueSoft, Surface),
)
