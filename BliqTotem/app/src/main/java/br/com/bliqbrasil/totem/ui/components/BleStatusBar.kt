package br.com.bliqbrasil.totem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import br.com.bliqbrasil.totem.ui.theme.*

private data class StatusConfig(
    val dotColor: Color,
    val label: String,
    val showSpinner: Boolean,
    val showRetry: Boolean,
)

private fun configFor(status: ConnectionStatus) = when (status) {
    ConnectionStatus.DISCONNECTED -> StatusConfig(Tertiary, "Conectando ao CLP...",  true,  false)
    ConnectionStatus.SCANNING     -> StatusConfig(Warning,  "Procurando CLP...",      true,  false)
    ConnectionStatus.CONNECTING   -> StatusConfig(Warning,  "Conectando...",          true,  false)
    ConnectionStatus.CONNECTED    -> StatusConfig(Success,  "CLP conectado",          false, false)
    ConnectionStatus.RECONNECTING -> StatusConfig(Warning,  "Reconectando ao CLP...", true,  false)
    ConnectionStatus.ERROR        -> StatusConfig(Error,    "Falha na conexão",         false, true)
}

@Composable
fun BleStatusBar(
    status: ConnectionStatus,
    errorMessage: String?,
    onForceRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = configFor(status)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (config.showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = config.dotColor,
                )
            } else {
                Box(modifier = Modifier.size(10.dp).background(config.dotColor, CircleShape))
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.label,
                    fontFamily = Epilogue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                )
                if (status == ConnectionStatus.ERROR && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontFamily = Epilogue,
                        fontSize = 11.sp,
                        color = Error,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            if (config.showRetry) {
                Button(
                    onClick = onForceRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text("Tentar agora", fontFamily = Epilogue, fontSize = 13.sp, color = Surface)
                }
            }
        }
    }
}
