package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.bliqbrasil.totem.R

val Epilogue = FontFamily(Font(R.font.epilogue))
val FugazOne = FontFamily(Font(R.font.fugazone))

// Escala tipográfica calibrada para totem (leitura a 40–70 cm).
// FugazOne = destaque de marca; Epilogue = corpo.
val BliqTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FugazOne, fontSize = 64.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = FugazOne, fontSize = 52.sp, letterSpacing = (-1).sp),
    displaySmall  = TextStyle(fontFamily = FugazOne, fontSize = 42.sp),
    headlineLarge  = TextStyle(fontFamily = FugazOne, fontSize = 36.sp),
    headlineMedium = TextStyle(fontFamily = FugazOne, fontSize = 30.sp),
    headlineSmall  = TextStyle(fontFamily = FugazOne, fontSize = 26.sp),
    titleLarge  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Bold,     fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleSmall  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge   = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Normal,   fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall   = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Bold,     fontSize = 16.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp),
    labelSmall  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp),
)

// Estilos ad-hoc para elementos únicos do totem (preço, timer, badges).
object BliqTextStyles {
    val TimerHero  = TextStyle(fontFamily = FugazOne, fontSize = 88.sp, letterSpacing = (-3).sp, lineHeight = 96.sp)
    val PriceLarge = TextStyle(fontFamily = FugazOne, fontSize = 28.sp, letterSpacing = (-0.5).sp)
    val Eyebrow    = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
    val CtaLabel   = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.3.sp)
    val Wordmark   = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 32.sp, letterSpacing = (-0.5).sp)
}
