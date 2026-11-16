package br.com.bliqbrasil.totem.ui.screens.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
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
    onConfirm: (PaymentMethod, CheckoutViewModel.CupomAplicado?) -> Unit,
    onVerCupons: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val valorAPagar = state.valorAPagar(viewModel.totalPrice)
    // Cupom de 100%: não há o que cobrar, então não há forma de pagamento.
    val semCobranca = state.cupom != null && valorAPagar <= 0.0

    val palette = LocalBoxPalette.current

    // Layout pensado para caber inteiro na Sunmi P2 (360×720 dp): o título da
    // tela fica só na barra, o botão fica fixo no rodapé e os cards são enxutos.
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text("Resumo do pedido", fontFamily = FugazOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = palette.onContrast)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.contrast, titleContentColor = palette.onContrast),
            )
        },
        bottomBar = {
            // O Scaffold NÃO repassa contentWindowInsets ao bottomBar — ele
            // precisa consumir o inset da barra de navegação por conta própria.
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = BliqDimens.BottomSafeGap)
            ) {
                PrimaryButton(
                    label = if (semCobranca) "Confirmar" else "Confirmar pagamento",
                    onClick = {
                        val method = if (semCobranca) PaymentMethod.CUPOM else state.selectedMethod
                        method?.let { onConfirm(it, state.cupom) }
                    },
                    // Não deixa seguir enquanto um cupom está sendo validado: o
                    // total ainda pode mudar.
                    enabled = (semCobranca || state.selectedMethod != null) && !state.validandoCupom,
                )
            }
        },
        containerColor = palette.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ServicoCard(viewModel) }

            if (viewModel.aceitaCupom) {
                item {
                    CupomCard(
                        state = state,
                        onCodigoChange = viewModel::updateCodigo,
                        onAplicar = { viewModel.aplicarCupom() },
                        onRemover = viewModel::removerCupom,
                        onVerCupons = onVerCupons,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    state.cupom?.let { cupom ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", fontFamily = Epilogue, color = palette.onBackground, fontSize = 14.sp)
                            Text(
                                formatCurrency(viewModel.totalPrice),
                                fontFamily = Epilogue,
                                color = palette.onBackground,
                                fontSize = 14.sp,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Desconto (${cupom.codigo})", fontFamily = Epilogue, color = palette.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("− ${formatCurrency(cupom.desconto)}", fontFamily = Epilogue, color = palette.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total a pagar", fontFamily = Epilogue, color = palette.onBackground, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                        Text(formatCurrency(valorAPagar), fontFamily = FugazOne, color = palette.onBackground, fontSize = 28.sp)
                    }
                }
            }

            if (!semCobranca) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FORMA DE PAGAMENTO", fontSize = 12.sp, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = palette.onBackground, letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        }
    }
}

/**
 * Serviço e tempo total numa linha só. O detalhamento (tempo base + extras)
 * só aparece quando há extras — sem eles o tempo base é o próprio total.
 */
@Composable
private fun ServicoCard(viewModel: CheckoutViewModel) {
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Alinhado pelo topo para os dois rótulos ficarem na mesma linha.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("SERVIÇO", fontSize = 11.sp, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = Tertiary, letterSpacing = 0.5.sp)
                    Text(viewModel.washOption.label, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = OnSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TEMPO TOTAL", fontSize = 11.sp, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = Tertiary, letterSpacing = 0.5.sp)
                    Text("${viewModel.totalMinutes} min", fontFamily = FugazOne, fontSize = 22.sp, color = Primary)
                }
            }
            if (viewModel.selectedExtras.isNotEmpty()) {
                HorizontalDivider(color = Divider)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tempo base", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                    Text("${viewModel.washOption.minutes} min", fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnSurface)
                }
                viewModel.selectedExtras.forEach { extra ->
                    // O rótulo cadastrado costuma ser só "+N min" — repetiria o valor ao lado.
                    val rotuloSoMinutos = extra.rotulo.filter { it.isLetterOrDigit() }.lowercase() in
                        setOf("${extra.minutos}min", "${extra.minutos}minutos")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (rotuloSoMinutos) "Tempo extra" else extra.rotulo, fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                        Text("+ ${extra.minutos} min · ${formatCurrency(extra.preco)}", fontFamily = Epilogue, color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
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
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BliqIconChip(
                icon = option.icon,
                size = 40.dp,
                tint = if (isSelected) palette.onContrast else palette.accent,
                background = if (isSelected) palette.accent else palette.chipSoft,
                contentDescription = option.label,
            )
            Text(
                option.label,
                fontFamily = Epilogue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) palette.onContrast else palette.onSurface,
            )
        }
    }
}

@Composable
private fun CupomCard(
    state: CheckoutViewModel.UiState,
    onCodigoChange: (String) -> Unit,
    onAplicar: () -> Unit,
    onRemover: () -> Unit,
    onVerCupons: () -> Unit,
) {
    val palette = LocalBoxPalette.current
    val keyboard = LocalSoftwareKeyboardController.current

    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val cupom = state.cupom
            // Link na mesma linha do título para economizar uma linha inteira.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("CUPOM DE DESCONTO", fontSize = 11.sp, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = Tertiary, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
                if (cupom == null) {
                    TextButton(
                        onClick = onVerCupons,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp),
                    ) {
                        Text("Ver cupons", fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = palette.accent)
                    }
                }
            }
            if (cupom != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BliqIconChip(
                        icon = BliqIcons.Cupom,
                        size = 36.dp,
                        tint = palette.accent,
                        background = palette.chipSoft,
                        contentDescription = null,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(cupom.codigo, fontFamily = Epilogue, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OnSurface)
                        Text(
                            cupom.descricao ?: "− ${formatCurrency(cupom.desconto)}",
                            fontFamily = Epilogue, fontSize = 13.sp, color = Secondary,
                        )
                    }
                    TextButton(onClick = onRemover) {
                        Text("Remover", fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = palette.accent)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.codigoDigitado,
                        onValueChange = onCodigoChange,
                        placeholder = { Text("Digite o código", fontFamily = Epilogue, color = Tertiary) },
                        singleLine = true,
                        isError = state.erroCupom != null,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Epilogue, fontSize = 16.sp, color = OnSurface),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onAplicar() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(54.dp),
                    )
                    Button(
                        onClick = { keyboard?.hide(); onAplicar() },
                        enabled = state.codigoDigitado.isNotBlank() && !state.validandoCupom,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.contrast, contentColor = palette.onContrast),
                        modifier = Modifier.height(54.dp),
                    ) {
                        if (state.validandoCupom) {
                            CircularProgressIndicator(color = palette.onContrast, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Aplicar", fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
                state.erroCupom?.let {
                    Text(it, fontFamily = Epilogue, fontSize = 13.sp, color = Error)
                }
            }
        }
    }
}
