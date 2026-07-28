package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val colorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = Surface,
    primaryContainer   = PrimaryLight,
    onPrimaryContainer = PrimaryDeep,
    secondary          = Secondary,
    onSecondary        = Surface,
    background         = Background,
    onBackground       = OnSurface,
    surface            = Surface,
    onSurface          = OnSurface,
    surfaceVariant     = PrimaryLight,
    onSurfaceVariant   = Secondary,
    error              = Error,
    onError            = Surface,
    errorContainer     = ErrorSurface,
    outline            = Divider,
)

@Composable
fun BliqTotemTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = BliqTypography,
        shapes      = BliqShapes,
        content     = content,
    )
}
