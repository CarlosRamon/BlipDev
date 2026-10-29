package br.com.bliqbrasil.totem.ui.screens.cupons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.data.model.CupomDisponivel
import br.com.bliqbrasil.totem.ui.components.BliqIconChip
import br.com.bliqbrasil.totem.ui.components.BliqIcons
import br.com.bliqbrasil.totem.ui.components.OutlineButton
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// validoAte chega em ISO UTC; exibe o dia em Brasília. SimpleDateFormat porque
// java.time exige API 26 e o minSdk é 22.
private fun diaBrt(iso: String): String? = runCatching {
    val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val saida = SimpleDateFormat("dd/MM", Locale("pt", "BR")).apply { timeZone = TimeZone.getTimeZone("America/Sao_Paulo") }
    saida.format(entrada.parse(iso)!!)
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuponsScreen(
    viewModel: CuponsViewModel,
    onBack: () -> Unit,
    onEscolher: (codigo: String) -> Unit,
) {
    val palette = LocalBoxPalette.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text("Cupons", fontFamily = FugazOne) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Text(
                "CUPONS DISPONÍVEIS",
                fontSize = 28.sp,
                fontFamily = FugazOne,
                color = palette.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            if (!viewModel.identificado) {
                Text(
                    "Identifique-se com CPF para ver cupons exclusivos para você.",
                    fontFamily = Epilogue,
                    fontSize = 15.sp,
                    color = palette.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            when (val s = state) {
                CuponsViewModel.UiState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.onBackground)
                }

                CuponsViewModel.UiState.Error -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Mensagem("Não foi possível carregar os cupons.")
                    OutlineButton(label = "Tentar de novo", onClick = viewModel::carregar)
                }

                is CuponsViewModel.UiState.Loaded -> if (s.cupons.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Mensagem("Nenhum cupom disponível no momento.")
                        OutlineButton(label = "Voltar", onClick = onBack)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + BliqDimens.BottomSafeGap),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.cupons, key = { it.codigo }) { cupom ->
                            CupomItem(cupom, onClick = { onEscolher(cupom.codigo) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Mensagem(texto: String) {
    Text(
        texto,
        fontFamily = Epilogue,
        fontSize = 17.sp,
        color = LocalBoxPalette.current.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CupomItem(cupom: CupomDisponivel, onClick: () -> Unit) {
    val palette = LocalBoxPalette.current
    val selo = if (cupom.tipoDesconto == "PERCENTUAL") {
        "${cupom.valor.toBigDecimal().stripTrailingZeros().toPlainString().replace('.', ',')}% OFF"
    } else {
        "${formatCurrency(cupom.valor)} OFF"
    }
    val validade = cupom.validoAte?.let(::diaBrt)?.let { "Válido até $it" }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BliqIconChip(icon = BliqIcons.Cupom, size = 52.dp, tint = palette.accent, background = palette.chipSoft)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(selo, fontFamily = FugazOne, fontSize = 20.sp, color = palette.accent)
                Text(cupom.codigo, fontFamily = Epilogue, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface)
                cupom.descricao?.let { Text(it, fontFamily = Epilogue, fontSize = 14.sp, color = Secondary) }
                validade?.let { Text(it, fontFamily = Epilogue, fontSize = 13.sp, color = Tertiary) }
            }
            cupom.valorFinal?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Você paga", fontFamily = Epilogue, fontSize = 12.sp, color = Tertiary)
                    Text(formatCurrency(it), fontFamily = Epilogue, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnSurface)
                }
            }
        }
    }
}
