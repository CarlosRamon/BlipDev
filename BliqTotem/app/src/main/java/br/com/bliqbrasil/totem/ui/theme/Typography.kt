package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.bliqbrasil.totem.R

val Epilogue = FontFamily(Font(R.font.epilogue))
val FugazOne = FontFamily(Font(R.font.fugazone))

val BliqTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FugazOne, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = FugazOne, fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = FugazOne, fontSize = 36.sp),
    headlineLarge  = TextStyle(fontFamily = FugazOne, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FugazOne, fontSize = 28.sp),
    headlineSmall  = TextStyle(fontFamily = FugazOne, fontSize = 24.sp),
    titleLarge  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge   = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall   = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall  = TextStyle(fontFamily = Epilogue, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
