package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun schemeFor(palette: BoxPalette) = lightColorScheme(
    primary            = palette.accent,
    onPrimary          = Surface,
    primaryContainer   = PrimaryLight,
    onPrimaryContainer = PrimaryDeep,
    secondary          = Secondary,
    onSecondary        = Surface,
    background         = palette.background,
    onBackground       = palette.onBackground,
    surface            = palette.surface,
    onSurface          = palette.onSurface,
    surfaceVariant     = PrimaryLight,
    onSurfaceVariant   = Secondary,
    error              = Error,
    onError            = Surface,
    errorContainer     = ErrorSurface,
    outline            = Divider,
)

/**
 * Tema do totem. O [boxTipo] decide a identidade cromática inteira — ver [BoxPalette].
 *
 * Telas e componentes leem a paleta por `LocalBoxPalette.current`; o `MaterialTheme`
 * recebe `background`/`onBackground` já invertidos para que Scaffold e Surface
 * peguem a cor certa sem alteração.
 */
@Composable
fun BliqTotemTheme(
    boxTipo: BoxTipo = BoxTipo.LAVACAO,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(boxTipo)
    CompositionLocalProvider(LocalBoxPalette provides palette) {
        MaterialTheme(
            colorScheme = schemeFor(palette),
            typography  = BliqTypography,
            shapes      = BliqShapes,
            content     = content,
        )
    }
}
