package br.com.bliqbrasil.totem.ui.screens.extras

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.ui.components.PrimaryButton
import br.com.bliqbrasil.totem.ui.components.Sparkle
import br.com.bliqbrasil.totem.ui.theme.*
import br.com.bliqbrasil.totem.util.formatCurrency
import kotlin.math.roundToInt

// Economia por minuto abaixo disso não vale um selo — vira ruído.
private const val MIN_SAVINGS_PERCENT = 5

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

    // O benefício que a tela vende: o minuto extra comparado ao minuto do pacote.
    val basePerMinute = if (washOption.minutes > 0) washOption.price / washOption.minutes else null
    val bestValueId = washOption.extras
        .takeIf { it.size > 1 }
        ?.filter { it.minutos > 0 }
        ?.minByOrNull { it.preco / it.minutos }
        ?.id

    val palette = LocalBoxPalette.current

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text("Extras", fontFamily = FugazOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = palette.onContrast)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.contrast, titleContentColor = palette.onContrast),
            )
        },
        containerColor = palette.background,
        bottomBar = {
            // O Scaffold NÃO repassa contentWindowInsets ao bottomBar — ele
            // precisa consumir o inset da barra de navegação por conta própria.
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = BliqDimens.BottomSafeGap),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TotalTimeCard(
                    baseMinutes = washOption.minutes,
                    extraMinutes = extraMinutes,
                    totalPrice = totalPrice,
                )
                PrimaryButton(
                    label = if (extraMinutes > 0) "Continuar com +$extraMinutes min" else "Continuar sem extras",
                    onClick = { onContinue(selectedExtras, totalMinutes, totalPrice) },
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Hero() }

            items(washOption.extras, key = { it.id }) { extra ->
                val isSelected = extra.id in selectedIds
                val savingsPercent = savingsPercent(basePerMinute, extra)
                ExtraCard(
                    extra = extra,
                    isSelected = isSelected,
                    isBestValue = extra.id == bestValueId,
                    savingsPercent = savingsPercent,
                    onToggle = {
                        selectedIds = if (isSelected) selectedIds - extra.id else selectedIds + extra.id
                    }
                )
            }
        }
    }
}

private fun savingsPercent(basePerMinute: Double?, extra: WashOptionExtra): Int? {
    if (basePerMinute == null || basePerMinute <= 0 || extra.minutos <= 0) return null
    val percent = ((1 - (extra.preco / extra.minutos) / basePerMinute) * 100).roundToInt()
    return percent.takeIf { it >= MIN_SAVINGS_PERCENT }
}

@Composable
private fun Hero() {
    val palette = LocalBoxPalette.current
    val servico = if (palette.tipo == BoxTipo.ASPIRACAO) "aspiração" else "lavagem"

    val pulse = rememberInfiniteTransition(label = "sparkle")
    val sparkleScale by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sparkleScale",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Sparkle(size = 28.dp, color = Warning, modifier = Modifier.scale(sparkleScale))
            Text("TURBINE SEU TEMPO", fontFamily = FugazOne, fontSize = 28.sp, color = palette.onBackground)
        }
        Text(
            "Mais minutos para caprichar na sua $servico.",
            fontFamily = Epilogue,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = palette.onBackground,
        )
    }
}

@Composable
private fun ExtraCard(
    extra: WashOptionExtra,
    isSelected: Boolean,
    isBestValue: Boolean,
    savingsPercent: Int?,
    onToggle: () -> Unit,
) {
    val palette = LocalBoxPalette.current

    val cardColor by animateColorAsState(if (isSelected) palette.contrast else palette.surface, label = "cardColor")
    val titleColor = if (isSelected) palette.onContrast else palette.accent
    val bodyColor  = if (isSelected) palette.onContrast.copy(alpha = 0.85f) else Secondary
    // O rótulo cadastrado costuma ser só "+N min" — repetiria o selo ao lado.
    val showRotulo = extra.rotulo.filter { it.isLetterOrDigit() }.lowercase() !in
        setOf("${extra.minutos}min", "${extra.minutos}minutos")
    // Um "pulo" curto ao marcar, para o toque ter resposta física.
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale",
    )

    Box {
        Card(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isBestValue) 12.dp else 0.dp)
                .scale(scale),
            shape = RoundedCornerShape(18.dp),
            border = if (isSelected) BorderStroke(3.dp, Warning) else null,
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 3.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = if (isBestValue) 22.dp else 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MinutesBadge(minutes = extra.minutos, isSelected = isSelected)

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (showRotulo) {
                            Text(extra.rotulo.uppercase(), fontFamily = FugazOne, fontSize = 17.sp, color = titleColor)
                        }
                        Text("por mais", fontFamily = Epilogue, fontSize = 15.sp, color = bodyColor)
                        Text(formatCurrency(extra.preco), style = BliqTextStyles.PriceLarge, color = titleColor)
                    }
                }

                if (savingsPercent != null) {
                    Surface(shape = RoundedCornerShape(50), color = SuccessLight) {
                        Text(
                            "Minuto $savingsPercent% mais barato",
                            fontFamily = Epilogue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BliqNavy,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                AddPill(isSelected = isSelected)
            }
        }

        if (isBestValue) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Warning,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Sparkle(size = 12.dp, color = BliqNavy)
                    Text("MELHOR CUSTO-BENEFÍCIO", fontFamily = Epilogue, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.5.sp, color = BliqNavy)
                }
            }
        }
    }
}

@Composable
private fun MinutesBadge(minutes: Int, isSelected: Boolean) {
    val palette = LocalBoxPalette.current
    val container = if (isSelected) Warning else palette.accent
    val content   = if (isSelected) BliqNavy else Surface
    Column(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("+$minutes", fontFamily = FugazOne, fontSize = 28.sp, lineHeight = 30.sp, color = content)
        Text("MIN", fontFamily = Epilogue, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp, color = content)
    }
}

@Composable
private fun AddPill(isSelected: Boolean) {
    val palette = LocalBoxPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Warning else palette.accent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (isSelected) "✓ ADICIONADO" else "+ ADICIONAR AO MEU TEMPO",
            fontFamily = Epilogue,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp,
            color = if (isSelected) BliqNavy else Surface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

/**
 * Barra do tempo total: o trecho do pacote e, em destaque, o que os extras
 * acrescentam. Deixa o ganho visível antes do pagamento.
 */
@Composable
private fun TotalTimeCard(baseMinutes: Int, extraMinutes: Int, totalPrice: Double) {
    val palette = LocalBoxPalette.current
    val totalMinutes = baseMinutes + extraMinutes
    val extraFraction by animateFloatAsState(
        targetValue = if (totalMinutes > 0) extraMinutes.toFloat() / totalMinutes else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "extraFraction",
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("TEMPO TOTAL", style = BliqTextStyles.Eyebrow, color = Secondary)
                    AnimatedContent(
                        targetState = totalMinutes,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "totalMinutes",
                    ) { minutes ->
                        Text("$minutes min", fontFamily = FugazOne, fontSize = 26.sp, color = palette.onSurface)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL", style = BliqTextStyles.Eyebrow, color = Secondary)
                    Text(formatCurrency(totalPrice), fontFamily = FugazOne, fontSize = 26.sp, color = palette.accent)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BliqBlueSoft),
            ) {
                Box(
                    Modifier
                        .weight(1f - extraFraction + 0.0001f)
                        .fillMaxHeight()
                        .background(BliqBlue)
                )
                if (extraFraction > 0f) {
                    Box(
                        Modifier
                            .weight(extraFraction)
                            .fillMaxHeight()
                            .background(Warning)
                    )
                }
            }
        }
    }
}

