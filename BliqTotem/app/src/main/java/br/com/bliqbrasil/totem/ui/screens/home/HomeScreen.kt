package br.com.bliqbrasil.totem.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import br.com.bliqbrasil.totem.BuildConfig
import br.com.bliqbrasil.totem.R
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import br.com.bliqbrasil.totem.data.model.PosConfig
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.ui.components.BleStatusBar
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToExtras: (WashOption, boxTipo: String) -> Unit,
    onNavigateToCheckout: (WashOption, List<WashOptionExtra>, Int, Double, boxTipo: String) -> Unit,
    onNavigateToMinutesPicker: (WashOption, boxTipo: String) -> Unit,
    onNavigateToSession: (cicloId: String, totalMinutes: Int, resumeFromSeconds: Int?, boxTipo: String) -> Unit,
    onNeedActivation: () -> Unit,
    onNavigateToSupport: () -> Unit,
) {
    val state     by viewModel.state.collectAsStateWithLifecycle()
    val bleStatus by viewModel.bleManager.status.collectAsStateWithLifecycle()
    val bleError  by viewModel.bleManager.errorMessage.collectAsStateWithLifecycle()
    val bleManager = viewModel.bleManager

    var logoTapCount by remember { mutableStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(logoTapCount) {
        if (logoTapCount in 1..6) {
            kotlinx.coroutines.delay(2_000)
            logoTapCount = 0
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) bleManager.startAutoConnect()
    }

    LaunchedEffect(Unit) {
        if (bleManager.hasPermissions()) bleManager.startAutoConnect()
        else permLauncher.launch(bleManager.neededPermissions())
    }

    LaunchedEffect(state.needsActivation) {
        if (state.needsActivation) onNeedActivation()
    }

    LaunchedEffect(state.activeSession) {
        state.activeSession?.let { session ->
            viewModel.consumeActiveSession()
            onNavigateToSession(session.cicloId, session.totalMinutes, session.resumeFromSeconds, session.boxTipo)
        }
    }

    if (state.showWelcome) {
        WelcomeContent(
            config    = state.config,
            bleStatus = bleStatus,
            onStart   = viewModel::dismissWelcome,
        )
        return
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

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = onNavigateToSupport) {
                        Text("Ajuda", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(32.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = painterResource(R.drawable.bliq_tagline),
                        contentDescription = "Bliq",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(bottom = 16.dp)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    logoTapCount++
                                    if (logoTapCount >= 7) {
                                        logoTapCount = 0
                                        showResetDialog = true
                                    }
                                }
                            },
                    )
                    state.config?.let { cfg ->
                        Text(
                            text = cfg.box.nome,
                            fontSize = 20.sp,
                            fontFamily = FugazOne,
                            color = OnSurface,
                        )
                    }
                    Text(
                        text = if (state.boxTipo == "ASPIRACAO") "Selecione a aspiração" else "Selecione a lavagem",
                        fontSize = 15.sp,
                        fontFamily = Epilogue,
                        color = Secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    state.config?.let { cfg ->
                        Text(
                            text = cfg.franqueado.nome,
                            fontSize = 12.sp,
                            fontFamily = Epilogue,
                            color = Tertiary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                BleStatusBar(
                    status = bleStatus,
                    errorMessage = bleError,
                    onForceRetry = { bleManager.startAutoConnect(force = true) },
                )
            }

            if (!BuildConfig.STONE_ENABLED) {
                item { BleDebugCard(bleManager.debugInfo(), state.boxTipo) }
            }

            when {
                state.loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(Modifier.height(12.dp))
                            Text("Carregando...", fontSize = 14.sp, fontFamily = Epilogue, color = Secondary)
                        }
                    }
                }

                state.error != null -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                state.error!!,
                                fontFamily = Epilogue,
                                color = Error,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = viewModel::load,
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            ) {
                                Text("Tentar novamente", fontFamily = Epilogue, color = Surface)
                            }
                        }
                    }
                }

                else -> items(state.washOptions, key = { it.id }) { option ->
                    ProductCard(option = option, onClick = {
                        val boxTipo = state.boxTipo
                        when {
                            option.tipo == "MINUTAGEM_AVULSA" -> onNavigateToMinutesPicker(option, boxTipo)
                            option.extras.isNotEmpty() -> onNavigateToExtras(option, boxTipo)
                            else -> onNavigateToCheckout(option, emptyList(), option.minutes, option.price, boxTipo)
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    config: PosConfig?,
    bleStatus: ConnectionStatus,
    onStart: () -> Unit,
) {
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
        modifier = Modifier
            .fillMaxSize()
            .background(Primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            Image(
                painter          = painterResource(R.drawable.bliq_tagline),
                contentDescription = "Bliq",
                modifier         = Modifier.fillMaxWidth(0.65f),
                colorFilter      = ColorFilter.tint(Surface),
            )

            config?.let { cfg ->
                Text(
                    text       = cfg.box.nome,
                    fontSize   = 18.sp,
                    fontFamily = FugazOne,
                    color      = Surface.copy(alpha = 0.8f),
                    textAlign  = TextAlign.Center,
                )
            }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(scale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Surface,
                    contentColor   = Primary,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    "Toque para iniciar",
                    fontFamily  = Epilogue,
                    fontSize    = 17.sp,
                    fontWeight  = FontWeight.SemiBold,
                )
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
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(dot,   fontSize = 10.sp, color = Surface.copy(alpha = 0.5f))
            Text(label, fontFamily = Epilogue, fontSize = 13.sp, color = Surface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun BleDebugCard(info: Map<String, String>, boxTipo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1A1A2E)),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "🔧 DEBUG BLE",
                fontFamily = Epilogue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF00E5FF),
            )
            Text(
                "Box tipo: $boxTipo",
                fontFamily = Epilogue,
                fontSize = 10.sp,
                color = androidx.compose.ui.graphics.Color(0xFFB0BEC5),
            )
            info.forEach { (key, value) ->
                val valueColor = when {
                    value == "OK"     -> androidx.compose.ui.graphics.Color(0xFF69F0AE)
                    value == "NEGADA" -> androidx.compose.ui.graphics.Color(0xFFFF5252)
                    else              -> androidx.compose.ui.graphics.Color(0xFFFFD740)
                }
                Row {
                    Text(
                        "$key: ",
                        fontFamily = Epilogue,
                        fontSize = 10.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF78909C),
                    )
                    Text(
                        value,
                        fontFamily = Epilogue,
                        fontSize = 10.sp,
                        color = valueColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(option: WashOption, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(Primary, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(option.label, fontSize = 17.sp, fontFamily = FugazOne, color = OnSurface)
                if (option.tipo == "MINUTAGEM_AVULSA") {
                    Text("tempo à escolha", fontSize = 13.sp, fontFamily = Epilogue, color = Secondary)
                } else {
                    Text("${option.minutes} minutos", fontSize = 13.sp, fontFamily = Epilogue, color = Secondary)
                    if (option.extras.isNotEmpty()) {
                        val n = option.extras.size
                        Text(
                            text = "$n opção${if (n > 1) "ões" else ""} de extra",
                            fontSize = 11.sp,
                            fontFamily = Epilogue,
                            color = Primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (option.tipo == "MINUTAGEM_AVULSA") {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatCurrency(option.price), fontSize = 20.sp, fontFamily = FugazOne, color = Primary)
                        Text("/ min", fontSize = 11.sp, fontFamily = Epilogue, color = Secondary)
                    }
                } else {
                    Text(formatCurrency(option.price), fontSize = 20.sp, fontFamily = FugazOne, color = Primary)
                }
                Spacer(Modifier.width(8.dp))
                Text("›", fontSize = 24.sp, fontFamily = Epilogue, color = Tertiary)
            }
        }
    }
}
