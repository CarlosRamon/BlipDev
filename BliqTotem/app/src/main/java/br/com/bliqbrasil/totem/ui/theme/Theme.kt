package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val colorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = Surface,
    background       = Background,
    onBackground     = OnSurface,
    surface          = Surface,
    onSurface        = OnSurface,
    error            = Error,
    onError          = Surface,
)

@Composable
fun BliqTotemTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = BliqTypography,
        content     = content,
    )
}
