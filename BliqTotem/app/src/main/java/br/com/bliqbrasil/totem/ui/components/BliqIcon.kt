package br.com.bliqbrasil.totem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.bliqbrasil.totem.data.model.Machine
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.bliqbrasil.totem.ui.theme.Primary
import br.com.bliqbrasil.totem.ui.theme.PrimaryLight
import br.com.bliqbrasil.totem.ui.theme.SquircleShape

/**
 * Ícones semânticos do BliqTotem — substituem os emojis (💳 🏦 ⚡ 🔒 ✅ ❌ etc)
 * por vetoriais que renderizam idênticos em todos os fabricantes de POS.
 */
object BliqIcons {
    val Credit: ImageVector    = Icons.Filled.CreditCard
    val Debit: ImageVector     = Icons.Outlined.AccountBalance
    val Pix: ImageVector       = Icons.Filled.Bolt
    val QrCode: ImageVector    = Icons.Filled.QrCode2
    val Contactless: ImageVector = Icons.Filled.Contactless
    val Lock: ImageVector      = Icons.Filled.Lock
    val Check: ImageVector     = Icons.Filled.Check
    val CheckCircle: ImageVector = Icons.Filled.CheckCircle
    val Close: ImageVector     = Icons.Filled.Close
    val Cancel: ImageVector    = Icons.Filled.Cancel
    val Error: ImageVector     = Icons.Filled.ErrorOutline
    val Help: ImageVector      = Icons.Filled.HelpOutline
    val Speed: ImageVector     = Icons.Filled.Speed
    val Touch: ImageVector     = Icons.Filled.TouchApp

    // Ícones das máquinas do box — substituem os emoji do enum Machine,
    // que o brandbook proíbe na interface.
    val PreLavagem: ImageVector   = Icons.Filled.WaterDrop
    val Shampoo: ImageVector      = Icons.Filled.BubbleChart
    val Enxague: ImageVector      = Icons.Filled.Shower
    val Aspirador: ImageVector    = Icons.Filled.Air
    val ArComprimido: ImageVector = Icons.Filled.Waves
}

/**
 * BliqIconChip — ícone dentro de uma pastilha Squircle, no estilo Bliq.
 * Ideal para cards de método de pagamento, status e ações principais.
 */
@Composable
fun BliqIconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    tint: Color = Primary,
    background: Color = PrimaryLight,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(SquircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(size * 0.55f)
                .padding(0.dp),
        )
    }
}

/** Ícone vetorial de cada máquina, conforme os mockups do Figma. */
fun machineIcon(machine: Machine): ImageVector = when (machine) {
    Machine.PRE_LAVAGEM   -> BliqIcons.PreLavagem
    Machine.SHAMPOO       -> BliqIcons.Shampoo
    Machine.ENXAGUE       -> BliqIcons.Enxague
    Machine.ASPIRADOR     -> BliqIcons.Aspirador
    Machine.AR_COMPRIMIDO -> BliqIcons.ArComprimido
}
