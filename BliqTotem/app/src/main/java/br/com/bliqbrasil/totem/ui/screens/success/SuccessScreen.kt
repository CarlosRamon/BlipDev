package br.com.bliqbrasil.totem.ui.screens.success

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.data.model.PaymentMethod
import br.com.bliqbrasil.totem.ui.components.OutlineButton
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessScreen(
    viewModel: SuccessViewModel,
    paymentMethod: PaymentMethod,
    totalMinutes: Int,
    totalPrice: Double,
    onSessionReady: (cicloId: String) -> Unit,
    onBackToHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.sessionReady) {
        if (state.sessionReady) onSessionReady(viewModel.cicloId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagamento Confirmado", fontFamily = FugazOne) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Surface),
                navigationIcon = {},
            )
        },
        containerColor = Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.weight(1f))

                Surface(
                    shape = CircleShape,
                    color = SuccessLight,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✅", fontSize = 48.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Pagamento realizado\ncom sucesso!",
                    fontFamily = FugazOne,
                    fontSize = 24.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                )

                Spacer(Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Forma de pagamento", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text(
                                text = when (paymentMethod) {
                                    PaymentMethod.CREDIT -> "Cartão de Crédito"
                                    PaymentMethod.DEBIT  -> "Cartão de Débito"
                                    PaymentMethod.PIX    -> "Pix"
                                },
                                fontFamily = Epilogue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                        }
                        HorizontalDivider(color = Divider)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tempo liberado", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text("$totalMinutes minutos", fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = Divider)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Valor cobrado", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text(formatCurrency(totalPrice), fontFamily = FugazOne, color = Primary, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                val isError = state.bleStep == BleStep.ERROR
                Surface(
                    color = if (isError) ErrorSurface else Background.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val (showSpinner, icon, text, color) = when (state.bleStep) {
                                BleStep.IDLE        -> listOf(false, "⏳", "Aguardando...", Tertiary)
                                BleStep.CONNECTING  -> listOf(true,  "",   "Conectando ao dispositivo...", Warning)
                                BleStep.REGISTERING -> listOf(true,  "",   "Registrando ciclo...", Primary)
                                BleStep.SUCCESS     -> listOf(true,  "",   "Tudo certo! Abrindo painel...", Success)
                                BleStep.ERROR       -> listOf(false, "❌", state.bleError ?: "Erro desconhecido.", Error)
                            }
                            @Suppress("UNCHECKED_CAST")
                            if (showSpinner as Boolean) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = color as androidx.compose.ui.graphics.Color)
                            } else {
                                Text(icon as String, fontSize = 18.sp)
                            }
                            Text(
                                text as String,
                                fontFamily = Epilogue,
                                color = color as androidx.compose.ui.graphics.Color,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (isError) {
                            OutlineButton(label = "Tentar novamente", onClick = viewModel::retry)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }

            if (state.bleStep == BleStep.ERROR) {
                OutlineButton(label = "Voltar ao início", onClick = onBackToHome, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
