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
import br.com.bliqbrasil.totem.ui.theme.BliqTextStyles
import br.com.bliqbrasil.totem.ui.theme.BrandGradientHero
import br.com.bliqbrasil.totem.ui.theme.Primary
import br.com.bliqbrasil.totem.ui.theme.Surface

private val ButtonShape = RoundedCornerShape(16.dp)

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
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
                .background(if (active) BrandGradientHero else androidx.compose.ui.graphics.SolidColor(Primary.copy(alpha = 0.35f))),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Surface, strokeWidth = 2.5.dp)
            } else {
                Text(label, style = BliqTextStyles.CtaLabel, color = Surface)
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
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
    ) {
        Text(label, style = BliqTextStyles.CtaLabel)
    }
}
