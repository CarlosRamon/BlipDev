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
    val palette = LocalBoxPalette.current

    var minutes by remember { mutableIntStateOf(1) }
    val totalPrice = minutes * washOption.price

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text("Escolha o tempo", fontFamily = FugazOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Surface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.contrast, titleContentColor = palette.onContrast),
            )
        },
        containerColor = palette.background,
        bottomBar = {
            // O Scaffold NÃO repassa contentWindowInsets ao bottomBar — ele
            // precisa consumir o inset da barra de navegação por conta própria.
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = BliqDimens.BottomSafeGap)
            ) {
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
                Text(washOption.label.uppercase(), fontSize = 22.sp, fontFamily = FugazOne, color = palette.onBackground)
                Text(
                    "${formatCurrency(washOption.price)} por minuto",
                    fontSize = 16.sp,
                    fontFamily = Epilogue,
                    color = palette.onBackground.copy(alpha = 0.9f),
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
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.surface,
                            disabledContainerColor = palette.surface.copy(alpha = 0.4f),
                        ),
                        contentPadding = PaddingValues(0.dp),
                        enabled = minutes - STEP >= MIN_MINUTES,
                    ) {
                        Text("−", fontSize = 32.sp, fontFamily = FugazOne, color = palette.accent)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$minutes",
                            fontSize = 72.sp,
                            fontFamily = FugazOne,
                            color = palette.onBackground,
                            lineHeight = 80.sp,
                        )
                        Text("minutos", fontSize = 17.sp, fontFamily = Epilogue, color = palette.onBackground.copy(alpha = 0.9f))
                    }

                    FilledTonalButton(
                        onClick = { if (minutes + STEP <= MAX_MINUTES) minutes += STEP },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.surface,
                            disabledContainerColor = palette.surface.copy(alpha = 0.4f),
                        ),
                        contentPadding = PaddingValues(0.dp),
                        enabled = minutes + STEP <= MAX_MINUTES,
                    ) {
                        Text("+", fontSize = 32.sp, fontFamily = FugazOne, color = palette.accent)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.contrast),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total", fontFamily = Epilogue, color = palette.onContrast.copy(alpha = 0.85f), fontSize = 16.sp)
                        Text(formatCurrency(totalPrice), fontFamily = FugazOne, color = palette.onContrast, fontSize = 28.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
