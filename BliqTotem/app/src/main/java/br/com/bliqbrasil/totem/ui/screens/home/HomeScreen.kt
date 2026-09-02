package br.com.bliqbrasil.totem.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.BuildConfig
import br.com.bliqbrasil.totem.R
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.ui.components.BleStatusBar
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    clienteId: String,
    onNavigateToExtras: (WashOption, boxTipo: String, clienteId: String) -> Unit,
    onNavigateToCheckout: (WashOption, List<WashOptionExtra>, Int, Double, boxTipo: String, clienteId: String) -> Unit,
    onNavigateToMinutesPicker: (WashOption, boxTipo: String, clienteId: String) -> Unit,
    onNavigateToSession: (cicloId: String, totalMinutes: Int, resumeFromSeconds: Int?, boxTipo: String) -> Unit,
    onNeedActivation: () -> Unit,
    onNavigateToSupport: () -> Unit,
) {
    val state     by viewModel.state.collectAsStateWithLifecycle()
    val bleStatus by viewModel.bleManager.status.collectAsStateWithLifecycle()
    val bleError  by viewModel.bleManager.errorMessage.collectAsStateWithLifecycle()
    val palette = LocalBoxPalette.current
    val bleManager = viewModel.bleManager

    var logoTapCount by remember { mutableIntStateOf(0) }
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
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = onNavigateToSupport) {
                        Text("Ajuda", fontFamily = Epilogue, color = palette.onContrast, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.contrast),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            contentPadding = PaddingValues(bottom = 32.dp + BliqDimens.BottomSafeGap),
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
                        colorFilter = ColorFilter.tint(palette.logoTint),
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
                            text = cfg.box.nome.uppercase(),
                            fontSize = 34.sp,
                            lineHeight = 40.sp,
                            fontFamily = FugazOne,
                            color = palette.onBackground,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = if (state.boxTipo == "ASPIRACAO") "Selecione a aspiração:" else "Selecione a lavagem:",
                        fontSize = 17.sp,
                        fontFamily = Epilogue,
                        color = palette.onBackground,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    state.config?.let { cfg ->
                        Text(
                            text = cfg.franqueado.nome,
                            fontSize = 13.sp,
                            fontFamily = Epilogue,
                            color = palette.onBackground.copy(alpha = 0.75f),
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
                            CircularProgressIndicator(color = palette.onBackground)
                            Spacer(Modifier.height(12.dp))
                            Text("Carregando...", fontSize = 15.sp, fontFamily = Epilogue, color = palette.onBackground)
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
                    ProductCard(palette = palette, option = option, onClick = {
                        val boxTipo = state.boxTipo
                        when {
                            option.tipo == "MINUTAGEM_AVULSA" -> onNavigateToMinutesPicker(option, boxTipo, clienteId)
                            option.extras.isNotEmpty() -> onNavigateToExtras(option, boxTipo, clienteId)
                            else -> onNavigateToCheckout(option, emptyList(), option.minutes, option.price, boxTipo, clienteId)
                        }
                    })
                }
            }
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
private fun ProductCard(palette: BoxPalette, option: WashOption, onClick: () -> Unit) {
    // Minutagem avulsa é o card de destaque do Figma: pintado na cor de contraste
    // (navy na Lavação, azul na Aspiração) em vez de branco.
    val isHighlight = option.tipo == "MINUTAGEM_AVULSA"
    val cardColor   = if (isHighlight) palette.contrast else palette.surface
    val titleColor  = if (isHighlight) palette.onContrast else palette.accent
    val bodyColor   = if (isHighlight) palette.onContrast.copy(alpha = 0.85f) else palette.onSurface
    val priceColor  = if (isHighlight) palette.onContrast else palette.accent

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.label.uppercase(),
                    fontSize = 19.sp,
                    fontFamily = FugazOne,
                    color = titleColor,
                )
                if (isHighlight) {
                    Text("tempo à escolha", fontSize = 15.sp, fontFamily = Epilogue, color = bodyColor)
                } else {
                    Text("${option.minutes} minutos", fontSize = 15.sp, fontFamily = Epilogue, color = bodyColor)
                    if (option.extras.isNotEmpty()) {
                        val n = option.extras.size
                        Text(
                            text = "$n opção${if (n > 1) "ões" else ""} de extra",
                            fontSize = 14.sp,
                            fontFamily = Epilogue,
                            color = bodyColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHighlight) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatCurrency(option.price), fontSize = 24.sp, fontFamily = FugazOne, color = priceColor)
                        Text("/ min", fontSize = 12.sp, fontFamily = Epilogue, color = bodyColor)
                    }
                } else {
                    Text(formatCurrency(option.price), fontSize = 24.sp, fontFamily = FugazOne, color = priceColor)
                }
                Spacer(Modifier.width(10.dp))
                Text("\u203A", fontSize = 26.sp, fontFamily = Epilogue, color = priceColor)
            }
        }
    }
}
