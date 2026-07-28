package br.com.bliqbrasil.totem.ui.screens.minutespicker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.ui.components.PrimaryButton
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency

private const val STEP = 1
private const val MIN_MINUTES = 1
private const val MAX_MINUTES = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinutesPickerScreen(
    washOption: WashOption,
    onBack: () -> Unit,
    onContinue: (minutes: Int, totalPrice: Double) -> Unit,
) {
    var minutes by remember { mutableIntStateOf(1) }
    val totalPrice = minutes * washOption.price

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escolha o tempo", fontFamily = FugazOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Surface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Surface),
            )
        },
        containerColor = Background,
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                PrimaryButton(
                    label = "Continuar · ${formatCurrency(totalPrice)}",
                    onClick = { onContinue(minutes, totalPrice) },
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(washOption.label, fontSize = 20.sp, fontFamily = FugazOne, color = OnSurface)
                Text(
                    "${formatCurrency(washOption.price)} por minuto",
                    fontSize = 14.sp,
                    fontFamily = Epilogue,
                    color = Secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    FilledTonalButton(
                        onClick = { if (minutes - STEP >= MIN_MINUTES) minutes -= STEP },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface),
                        contentPadding = PaddingValues(0.dp),
                        enabled = minutes - STEP >= MIN_MINUTES,
                    ) {
                        Text("−", fontSize = 32.sp, fontFamily = FugazOne, color = Primary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$minutes",
                            fontSize = 64.sp,
                            fontFamily = FugazOne,
                            color = Primary,
                            lineHeight = 72.sp,
                        )
                        Text("minutos", fontSize = 16.sp, fontFamily = Epilogue, color = Secondary)
                    }

                    FilledTonalButton(
                        onClick = { if (minutes + STEP <= MAX_MINUTES) minutes += STEP },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface),
                        contentPadding = PaddingValues(0.dp),
                        enabled = minutes + STEP <= MAX_MINUTES,
                    ) {
                        Text("+", fontSize = 32.sp, fontFamily = FugazOne, color = Primary)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total", fontFamily = Epilogue, color = Surface.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(formatCurrency(totalPrice), fontFamily = FugazOne, color = Surface, fontSize = 28.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
