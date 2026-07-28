package br.com.bliqbrasil.totem.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.bliqbrasil.totem.ui.theme.BliqNavy

/**
 * Sparkle — a estrela de quatro pontas do logotipo Bliq (o "brilho"
 * ao lado do "Q"). Astroid de 4 pontas com curvas côncavas.
 */
@Composable
fun Sparkle(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = BliqNavy,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val armLen = w / 2f
        // Fator do "afundamento" das laterais (quanto menor, mais afiado)
        val waist = w * 0.14f

        val path = Path().apply {
            // ponta topo
            moveTo(cx, cy - armLen)
            // curva côncava até ponta direita
            quadraticBezierTo(cx + waist, cy - waist, cx + armLen, cy)
            // curva côncava até ponta baixo
            quadraticBezierTo(cx + waist, cy + waist, cx, cy + armLen)
            // curva côncava até ponta esquerda
            quadraticBezierTo(cx - waist, cy + waist, cx - armLen, cy)
            // fecha até topo
            quadraticBezierTo(cx - waist, cy - waist, cx, cy - armLen)
            close()
        }
        drawPath(path = path, color = color)
    }
}
