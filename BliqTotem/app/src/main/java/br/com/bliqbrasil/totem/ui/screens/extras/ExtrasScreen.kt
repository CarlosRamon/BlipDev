package br.com.bliqbrasil.totem.ui.screens.extras

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.ui.components.PrimaryButton
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasScreen(
    washOption: WashOption,
    onBack: () -> Unit,
    onContinue: (selected: List<WashOptionExtra>, totalMinutes: Int, totalPrice: Double) -> Unit,
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val selectedExtras = washOption.extras.filter { it.id in selectedIds }
    val extraMinutes   = selectedExtras.sumOf { it.minutos }
    val extraPrice     = selectedExtras.sumOf { it.preco }
    val totalMinutes   = washOption.minutes + extraMinutes
    val totalPrice     = washOption.price + extraPrice

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extras", fontFamily = FugazOne) },
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
                    label = "Continuar para pagamento",
                    onClick = { onContinue(selectedExtras, totalMinutes, totalPrice) },
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lavagem", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text(washOption.label, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = Divider)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tempo base", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text("${washOption.minutes} min", fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Valor base", fontFamily = Epilogue, color = Secondary, fontSize = 14.sp)
                            Text(formatCurrency(washOption.price), fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Text("OPÇÕES DISPONÍVEIS", fontSize = 13.sp, fontFamily = Epilogue, fontWeight = FontWeight.SemiBold, color = Tertiary, letterSpacing = 0.5.sp)
            }

            items(washOption.extras, key = { it.id }) { extra ->
                val isSelected = extra.id in selectedIds
                ExtraCard(
                    extra = extra,
                    isSelected = isSelected,
                    onToggle = {
                        selectedIds = if (isSelected) selectedIds - extra.id else selectedIds + extra.id
                    }
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tempo total", fontFamily = Epilogue, color = Surface.copy(alpha = 0.8f), fontSize = 14.sp)
                            Text("$totalMinutes min", fontFamily = Epilogue, color = Surface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Valor total", fontFamily = Epilogue, color = Surface.copy(alpha = 0.8f), fontSize = 14.sp)
                            Text(formatCurrency(totalPrice), fontFamily = FugazOne, color = Surface, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraCard(extra: WashOptionExtra, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, Primary) else null,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryLight else Surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    extra.rotulo,
                    fontFamily = FugazOne,
                    fontSize = 15.sp,
                    color = if (isSelected) Primary else OnSurface,
                )
                Text("+ ${extra.minutos} minutos", fontFamily = Epilogue, fontSize = 13.sp, color = Secondary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                formatCurrency(extra.preco),
                fontFamily = FugazOne,
                fontSize = 16.sp,
                color = if (isSelected) Primary else OnSurface,
            )
            Spacer(Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(2.dp, if (isSelected) Primary else Tertiary),
                color = if (isSelected) Primary else Surface,
                modifier = Modifier.size(24.dp),
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("✓", fontFamily = Epilogue, color = Surface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
