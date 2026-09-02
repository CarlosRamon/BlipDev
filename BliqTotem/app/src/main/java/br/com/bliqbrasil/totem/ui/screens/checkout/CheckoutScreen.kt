package br.com.bliqbrasil.totem.ui.screens.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.data.model.PaymentMethod
import br.com.bliqbrasil.totem.ui.components.BliqIconChip
import br.com.bliqbrasil.totem.ui.components.BliqIcons
import br.com.bliqbrasil.totem.ui.components.PrimaryButton
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency

private data class PaymentOption(val method: PaymentMethod, val label: String, val icon: ImageVector)

private val PAYMENT_OPTIONS = listOf(
    PaymentOption(PaymentMethod.CREDIT, "Crédito", BliqIcons.Credit),
    PaymentOption(PaymentMethod.DEBIT,  "Débito",  BliqIcons.Debit),
    PaymentOption(PaymentMethod.PIX,    "Pix",     BliqIcons.Pix),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
    onConfirm: (PaymentMethod) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val palette = LocalBoxPalette.current

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontFamily = FugazOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = palette.onContrast)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.contrast, titleContentColor = palette.onContrast),
            )
        },
        containerColor = palette.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + BliqDimens.BottomSafeGap),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("RESUMO DO PEDIDO", fontSize = 28.sp, fontFamily = FugazOne, color = palette.onBackground) }

            item {
                Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("DETALHES DO SERVIÇO", fontSize = 13.sp, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = Tertiary, letterSpacing = 0.5.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tipo", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text(viewModel.washOption.label, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = Divider)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tempo base", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text("${viewModel.washOption.minutes} min", fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        viewModel.selectedExtras.forEach { extra ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(extra.rotulo, fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                                Text("+ ${extra.minutos} min  ${formatCurrency(extra.preco)}", fontFamily = Epilogue, color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                        if (viewModel.selectedExtras.isNotEmpty()) HorizontalDivider(color = Divider)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tempo total", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text("${viewModel.totalMinutes} minutos", fontFamily = Epilogue, color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total a pagar:", fontFamily = Epilogue, color = palette.onBackground, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        Text(formatCurrency(viewModel.totalPrice), fontFamily = FugazOne, color = palette.onBackground, fontSize = 30.sp)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("FORMA DE PAGAMENTO", fontSize = 13.sp, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = palette.onBackground, letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PAYMENT_OPTIONS.forEach { option ->
                            PaymentCard(
                                option = option,
                                isSelected = state.selectedMethod == option.method,
                                onSelect = { viewModel.selectMethod(option.method) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                PrimaryButton(
                    label = "Confirmar Pagamento",
                    onClick = { state.selectedMethod?.let { onConfirm(it) } },
                    enabled = state.selectedMethod != null,
                )
            }
        }
    }
}

@Composable
private fun PaymentCard(
    option: PaymentOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBoxPalette.current
    Card(
        onClick = onSelect,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) palette.contrast else palette.surface,
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BliqIconChip(
                icon = option.icon,
                size = 52.dp,
                tint = if (isSelected) palette.onContrast else palette.accent,
                background = if (isSelected) palette.accent else palette.chipSoft,
                contentDescription = option.label,
            )
            Text(
                option.label,
                fontFamily = Epilogue,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) palette.onContrast else palette.onSurface,
            )
        }
    }
}
