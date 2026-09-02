package br.com.bliqbrasil.totem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import br.com.bliqbrasil.totem.ui.theme.BliqTextStyles
import br.com.bliqbrasil.totem.ui.theme.LocalBoxPalette

private val ButtonShape = RoundedCornerShape(16.dp)

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val palette = LocalBoxPalette.current
    val active = enabled && !loading
    Button(
        onClick = onClick,
        enabled = active,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = ButtonShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(ButtonShape)
                .background(SolidColor(if (active) palette.contrast else palette.contrast.copy(alpha = 0.35f))),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = palette.onContrast, strokeWidth = 2.5.dp)
            } else {
                Text(label, style = BliqTextStyles.CtaLabel, color = palette.onContrast.copy(alpha = if (active) 1f else 0.6f))
            }
        }
    }
}

@Composable
fun OutlineButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalBoxPalette.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.onBackground),
    ) {
        Text(label, style = BliqTextStyles.CtaLabel)
    }
}
