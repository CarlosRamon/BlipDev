package br.com.bliqbrasil.totem.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.bliqbrasil.totem.ui.theme.Surface

/**
 * Card padrão do sistema Bliq — cantos generosos (20dp), sombra leve,
 * superfície branca sobre o off-white institucional.
 */
@Composable
fun BliqCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Surface,
    elevation: androidx.compose.ui.unit.Dp = 3.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elev = CardDefaults.cardElevation(defaultElevation = elevation)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elev,
        ) { content() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elev,
        ) { content() }
    }
}
