package br.com.bliqbrasil.totem.ui.screens.payment

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.data.model.PaymentMethod
import br.com.bliqbrasil.totem.payment.stone.PaymentState
import br.com.bliqbrasil.totem.ui.components.OutlineButton
import br.com.bliqbrasil.totem.ui.components.PrimaryButton
import br.com.bliqbrasil.totem.ui.theme.*

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onSuccess: (cicloId: String, acquirerKey: String) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity

    LaunchedEffect(Unit) { viewModel.start(activity) }

    LaunchedEffect(uiState.cicloId) {
        val cicloId = uiState.cicloId ?: return@LaunchedEffect
        onSuccess(cicloId, uiState.acquirerKey ?: "")
    }

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.consumeNavigateBack()
            onBack()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState.paymentState) {
            is PaymentState.Idle,
            is PaymentState.WaitingCard -> WaitingCardContent(onCancel = { viewModel.cancel() })

            is PaymentState.WaitingQrCode -> WaitingQrCodeContent(
                state    = state,
                onCancel = { viewModel.cancel() },
            )

            is PaymentState.WaitingPassword -> StatusContent(
                icon     = "🔒",
                title    = "Digite sua senha",
                subtitle = "Use o teclado da maquininha",
            )

            is PaymentState.Sending,
            is PaymentState.WaitingRemoveCard -> StatusContent(
                icon     = "⚡",
                title    = "Processando...",
                subtitle = "Aguarde a confirmação",
            )

            is PaymentState.Success -> StatusContent(
                icon        = "✅",
                title       = "Aprovado!",
                subtitle    = if (uiState.isCreatingCiclo) "Registrando ciclo..." else "Concluído",
                showSpinner = uiState.isCreatingCiclo,
            )

            is PaymentState.Failure -> FailureContent(
                message = state.message.ifBlank { "Tente novamente" },
                onRetry = { viewModel.retry(activity) },
                onBack  = onBack,
            )

            is PaymentState.Cancelled -> StatusContent(
                icon     = "🚫",
                title    = "Cancelado",
                subtitle = "Pagamento cancelado",
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                shape = RoundedCornerShape(20.dp),
                color = Primary.copy(alpha = 0.1f),
            ) {
                Text(
                    text = when (viewModel.paymentMethod) {
                        PaymentMethod.CREDIT -> "💳 Crédito"
                        PaymentMethod.DEBIT  -> "🏦 Débito"
                        PaymentMethod.PIX    -> "⚡ Pix"
                    },
                    fontFamily = Epilogue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = Primary,
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WaitingCardContent(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = alpha * 0.15f)),
        ) {
            Text("💳", fontSize = 52.sp)
        }

        Text(
            text       = "Aproxime ou insira o cartão",
            fontFamily = FugazOne,
            fontSize   = 22.sp,
            color      = OnSurface,
            textAlign  = TextAlign.Center,
        )

        Text(
            text       = "Aguardando pagamento...",
            fontFamily = Epilogue,
            fontSize   = 15.sp,
            color      = Secondary,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))
        OutlineButton(label = "Cancelar", onClick = onCancel)
    }
}

@Composable
private fun WaitingQrCodeContent(state: PaymentState.WaitingQrCode, onCancel: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
    ) {
        Text(
            text       = "Escaneie o QR Code PIX",
            fontFamily = FugazOne,
            fontSize   = 22.sp,
            color      = OnSurface,
            textAlign  = TextAlign.Center,
        )

        val bitmap = state.qrBitmap
        if (bitmap != null) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "QR Code PIX",
                modifier           = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(12.dp),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface),
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }

        Text(
            text       = "Abra o app do seu banco e pague via PIX",
            fontFamily = Epilogue,
            fontSize   = 14.sp,
            color      = Secondary,
            textAlign  = TextAlign.Center,
        )

        OutlineButton(label = "Cancelar", onClick = onCancel)
    }
}

@Composable
private fun StatusContent(
    icon: String,
    title: String,
    subtitle: String,
    showSpinner: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text(icon, fontSize = 64.sp)
        Text(title, fontFamily = FugazOne, fontSize = 24.sp, color = OnSurface, textAlign = TextAlign.Center)
        Text(subtitle, fontFamily = Epilogue, fontSize = 15.sp, color = Secondary, textAlign = TextAlign.Center)
        if (showSpinner) {
            CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        }
    }
}

@Composable
private fun FailureContent(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text("❌", fontSize = 64.sp)
        Text("Não autorizada", fontFamily = FugazOne, fontSize = 24.sp, color = OnSurface, textAlign = TextAlign.Center)
        Text(message, fontFamily = Epilogue, fontSize = 15.sp, color = Secondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        PrimaryButton(label = "Tentar novamente", onClick = onRetry)
        OutlineButton(label = "Voltar ao checkout", onClick = onBack)
    }
}
