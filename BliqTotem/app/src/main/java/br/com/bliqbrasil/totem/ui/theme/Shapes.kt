package br.com.bliqbrasil.totem.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val BliqShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * SquircleShape — quadrado super arredondado (radius ~32% da altura),
 * o grafismo assinatura da marca Bliq. Usar em ícones-chip, cards de destaque
 * e badges. Para o efeito "pastilha inclinada" da apresentação, combinar com
 * `Modifier.graphicsLayer { rotationZ = -8f }` no container.
 */
object SquircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = size.minDimension * 0.32f
        return Outline.Rounded(
            RoundRect(
                left = 0f, top = 0f,
                right = size.width, bottom = size.height,
                cornerRadius = CornerRadius(r, r),
            )
        )
    }
}

/**
 * Dimensões de segurança do totem.
 *
 * [BottomSafeGap] é somado aos window insets nos CTAs de rodapé. Os insets
 * sozinhos não bastam: quando as system bars estão escondidas eles valem zero, e
 * a barra transiente (mostrada por swipe) é um overlay que não altera inset
 * nenhum. Essa folga fixa garante que o botão nunca fique sob a barra.
 */
object BliqDimens {
    val BottomSafeGap = 28.dp
}
