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
import br.com.bliqbrasil.totem.data.model.Machine
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showEndDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !state.isEnding) { showEndDialog = true }

    DisposableEffect(viewModel) {
        viewModel.setOnFinished(onFinished)
        onDispose { viewModel.setOnFinished {} }
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
        topBar = {
            TopAppBar(
                title = { Text(viewModel.sessionTitle, fontFamily = FugazOne) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Surface),
                navigationIcon = {},
            )
        },
        containerColor = Background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                val timerColor = when {
                    !state.sessionStarted  -> Tertiary
                    state.isPaused         -> Warning
                    state.remaining <= 60  -> Error
                    state.remaining <= 180 -> Warning
                    else                   -> Primary
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
                            val machineColor = Color(activeMachine.colorHex)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = machineColor.copy(alpha = 0.12f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(activeMachine.icon, fontSize = 16.sp)
                                    Text(activeMachine.label, fontFamily = Epilogue, color = machineColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(20.dp), color = Background) {
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

            if (state.isPaused) {
                item {
                    Surface(color = WarningLight, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            "Conexão com o CLP perdida. Aguardando reconexão — selecione o equipamento para retomar.",
                            fontFamily = Epilogue,
                            fontSize = 13.sp,
                            color = Warning,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                        )
                    }
                }
            }

            item {
                val label = if (viewModel.boxTipo == "ASPIRACAO") "SELECIONAR SERVIÇO" else "SELECIONAR EQUIPAMENTO"
                Text(label, fontFamily = Epilogue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Tertiary, letterSpacing = 0.5.sp)
            }

            items(viewModel.machines, key = { it.name }) { machine ->
                MachineCard(
                    machine = machine,
                    isActive = state.activeMachine == machine,
                    isDisabled = state.isEnding,
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
                        brush = androidx.compose.ui.graphics.SolidColor(if (state.isEnding) Tertiary else Error)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
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
    onClick: () -> Unit,
) {
    val machineColor = Color(machine.colorHex)
    Card(
        onClick = onClick,
        enabled = !isDisabled,
        shape = RoundedCornerShape(14.dp),
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, machineColor) else null,
        colors = CardDefaults.cardColors(containerColor = if (isActive) machineColor.copy(alpha = 0.07f) else Surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(machine.icon, fontSize = 28.sp)
                Text(
                    machine.label,
                    fontFamily = Epilogue,
                    fontSize = 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) machineColor else Secondary,
                )
            }
            if (isActive) {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .size(8.dp)
                        .background(machineColor, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}
