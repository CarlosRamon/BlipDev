package br.com.bliqbrasil.totem.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import br.com.bliqbrasil.totem.data.model.Machine
import br.com.bliqbrasil.totem.ui.components.BleStatusBar
import br.com.bliqbrasil.totem.ui.components.machineIcon
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bleStatus by viewModel.bleManager.status.collectAsStateWithLifecycle()
    val bleError  by viewModel.bleManager.errorMessage.collectAsStateWithLifecycle()
    var showEndDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = LocalBoxPalette.current

    BackHandler(enabled = !state.isEnding) { showEndDialog = true }

    DisposableEffect(viewModel) {
        viewModel.setOnFinished(onFinished)
        onDispose { viewModel.setOnFinished {} }
    }

    // Detecta transição isPaused true→false e exibe snackbar de reconexão
    var wasPaused by remember { mutableStateOf(false) }
    LaunchedEffect(state.isPaused) {
        if (state.isPaused) {
            wasPaused = true
        } else if (wasPaused) {
            wasPaused = false
            val msg = if (state.activeMachine != null)
                "Sessão retomada automaticamente."
            else
                "Equipamento reconectado. Selecione para continuar."
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("Encerrar sessão", fontFamily = FugazOne) },
            text  = { Text(viewModel.requestEndSession(), fontFamily = Epilogue) },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    viewModel.confirmEndSession()
                }) { Text("Encerrar", fontFamily = Epilogue, color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Cancelar", fontFamily = Epilogue)
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text(viewModel.sessionTitle, fontFamily = FugazOne) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.contrast, titleContentColor = palette.onContrast),
                navigationIcon = {},
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                    contentColor = Surface,
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        containerColor = palette.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp + BliqDimens.BottomSafeGap),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                val timerColor = when {
                    !state.sessionStarted  -> Tertiary
                    state.isPaused         -> Warning
                    state.remaining <= 60  -> Error
                    state.remaining <= 180 -> Warning
                    else                   -> palette.contrast
                }
                val timerLabel = when {
                    state.isEnding        -> "Encerrando..."
                    state.isPaused        -> "Sessão pausada"
                    !state.sessionStarted -> "Aguardando início"
                    else                  -> "Tempo restante"
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(timerLabel.uppercase(), fontFamily = Epilogue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Tertiary, letterSpacing = 0.5.sp)
                        Text(formatTime(state.remaining), fontFamily = FugazOne, fontSize = 72.sp, color = timerColor, letterSpacing = (-2).sp, lineHeight = 80.sp)

                        val activeMachine = state.activeMachine
                        if (activeMachine != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = palette.chipSoft,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(machineIcon(activeMachine), contentDescription = null, tint = palette.onChipSoft, modifier = Modifier.size(18.dp))
                                    Text(activeMachine.label, fontFamily = Epilogue, color = palette.onChipSoft, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(20.dp), color = palette.chipSoft) {
                                Text(
                                    if (viewModel.boxTipo == "ASPIRACAO") "Selecione um serviço" else "Selecione um equipamento",
                                    fontFamily = Epilogue,
                                    color = Tertiary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (bleStatus != ConnectionStatus.CONNECTED) {
                item {
                    BleStatusBar(
                        status = bleStatus,
                        errorMessage = bleError,
                        onForceRetry = { viewModel.bleManager.startAutoConnect(force = true) },
                    )
                }
            }

            item {
                val label = if (viewModel.boxTipo == "ASPIRACAO") "SELECIONAR SERVIÇO" else "SELECIONAR EQUIPAMENTO"
                Text(label, fontFamily = Epilogue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.onBackground.copy(alpha = 0.85f), letterSpacing = 0.5.sp)
            }

            items(viewModel.machines, key = { it.name }) { machine ->
                MachineCard(
                    machine = machine,
                    isActive = state.activeMachine == machine,
                    isDisabled = state.isEnding,
                    palette = palette,
                    onClick = { viewModel.selectMachine(machine) },
                )
            }

            if (state.bleError != null) {
                item {
                    Surface(color = ErrorSurface, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            state.bleError!!,
                            fontFamily = Epilogue,
                            color = Error,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { if (!state.isEnding) showEndDialog = true },
                    enabled = !state.isEnding,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (state.isEnding) Tertiary else palette.danger)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.danger),
                ) {
                    Text(
                        if (state.isEnding) "Encerrando..." else "Encerrar sessão",
                        fontFamily = Epilogue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MachineCard(
    machine: Machine,
    isActive: Boolean,
    isDisabled: Boolean,
    palette: BoxPalette,
    onClick: () -> Unit,
) {
    // Selecionado: card na cor de contraste (navy na Lavação, azul na Aspiração),
    // com a pastilha do ícone na cor do fundo da tela — a inversão do Figma.
    val cardColor  = if (isActive) palette.contrast else palette.surface
    val labelColor = if (isActive) palette.onContrast else palette.onSurface
    val chipColor  = if (isActive) palette.background else palette.iconChip
    val chipIcon   = if (isActive) palette.onBackground else palette.onIconChip

    Card(
        onClick = onClick,
        enabled = !isDisabled,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(chipColor, SquircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    machineIcon(machine),
                    contentDescription = null,
                    tint = chipIcon,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                machine.label,
                fontFamily = Epilogue,
                fontSize = 17.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = labelColor,
                modifier = Modifier.weight(1f),
            )
            if (isActive) {
                Box(modifier = Modifier.size(8.dp).background(palette.onContrast, CircleShape))
            } else {
                Text("\u203A", fontFamily = Epilogue, fontSize = 20.sp, color = Tertiary)
            }
        }
    }
}
