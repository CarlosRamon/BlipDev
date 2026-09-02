package br.com.bliqbrasil.totem.ui.screens.welcome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.R
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import br.com.bliqbrasil.totem.ui.components.BliqIcons
import br.com.bliqbrasil.totem.ui.theme.*

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel,
    onStartFlow: () -> Unit,
    onNeedActivation: () -> Unit,
    onResumeSession: (cicloId: String, totalMinutes: Int, resumeFromSeconds: Int?, boxTipo: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bleStatus by viewModel.bleManager.status.collectAsStateWithLifecycle()
    val palette = LocalBoxPalette.current

    var logoTapCount by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(logoTapCount) {
        if (logoTapCount in 1..6) {
            kotlinx.coroutines.delay(2_000)
            logoTapCount = 0
        }
    }

    LaunchedEffect(state.needsActivation) {
        if (state.needsActivation) onNeedActivation()
    }

    LaunchedEffect(state.activeSession) {
        state.activeSession?.let { session ->
            viewModel.consumeActiveSession()
            onResumeSession(session.cicloId, session.totalMinutes, session.resumeFromSeconds, session.boxTipo)
        }
    }

    LaunchedEffect(state.pingState) {
        if (state.pingState == WelcomeViewModel.PingState.READY) {
            viewModel.consumeReady()
            onStartFlow()
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Resetar terminal?", fontFamily = FugazOne) },
            text  = { Text("O terminal será desvinculado e voltará à tela de ativação.", fontFamily = Epilogue) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetTerminal()
                }) { Text("Resetar", color = Error, fontFamily = Epilogue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", fontFamily = Epilogue)
                }
            },
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(palette.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 48.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.bliq_tagline),
                contentDescription = "Bliq",
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            logoTapCount++
                            if (logoTapCount >= 7) {
                                logoTapCount = 0
                                showResetDialog = true
                            }
                        }
                    },
                colorFilter = ColorFilter.tint(palette.logoTint),
            )

            state.config?.let { cfg ->
                Text(
                    text       = cfg.box.nome.uppercase(),
                    fontSize   = 34.sp,
                    lineHeight = 40.sp,
                    fontFamily = FugazOne,
                    color      = palette.onBackground,
                    textAlign  = TextAlign.Center,
                )
            }

            when (state.pingState) {
                WelcomeViewModel.PingState.IDLE -> {
                    Button(
                        onClick  = viewModel::onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .scale(scale),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.ctaContainer,
                            contentColor   = palette.onCtaContainer,
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            "Toque para iniciar",
                            fontFamily = Epilogue,
                            fontSize   = 19.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                WelcomeViewModel.PingState.CONNECTING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color       = palette.onBackground,
                            strokeWidth = 3.dp,
                            modifier    = Modifier.size(40.dp),
                        )
                        Text(
                            "Conectando equipamento...",
                            fontFamily  = Epilogue,
                            fontSize    = 17.sp,
                            fontWeight  = FontWeight.SemiBold,
                            color       = palette.onBackground,
                            textAlign   = TextAlign.Center,
                        )
                    }
                }

                WelcomeViewModel.PingState.CONNECTED,
                WelcomeViewModel.PingState.READY -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Success, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = BliqIcons.Check, contentDescription = null, tint = Surface, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            "Equipamento conectado!",
                            fontFamily  = Epilogue,
                            fontSize    = 17.sp,
                            fontWeight  = FontWeight.SemiBold,
                            color       = palette.onBackground,
                            textAlign   = TextAlign.Center,
                        )
                    }
                }
            }
        }

        val (dot, label) = when (bleStatus) {
            ConnectionStatus.CONNECTED    -> "●" to "Equipamento conectado"
            ConnectionStatus.SCANNING,
            ConnectionStatus.CONNECTING   -> "◌" to "Conectando ao equipamento..."
            ConnectionStatus.RECONNECTING -> "◌" to "Reconectando..."
            else                          -> "○" to "Aguardando equipamento"
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(dot,   fontSize = 10.sp, color = palette.onBackground.copy(alpha = 0.6f))
            Text(label, fontFamily = Epilogue, fontSize = 13.sp, color = palette.onBackground.copy(alpha = 0.6f))
        }
    }
}
