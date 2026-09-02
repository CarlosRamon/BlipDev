package br.com.bliqbrasil.totem.ui.screens.cpfinput

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.data.model.PoliticaCpf
import br.com.bliqbrasil.totem.ui.components.OutlineButton
import br.com.bliqbrasil.totem.ui.components.PrimaryButton
import br.com.bliqbrasil.totem.ui.screens.cpfinput.CpfInputViewModel.Companion.maskCpf
import br.com.bliqbrasil.totem.ui.screens.cpfinput.CpfInputViewModel.Companion.validarCpf
import br.com.bliqbrasil.totem.ui.theme.*

// ── Visual transformations ─────────────────────────────────────────────────────

private class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val masked = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append('.')
                if (i == 9) append('-')
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            // '.' at original offset 3 and 6; '-' at offset 9
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 5 -> minOf(offset + 1, masked.length)
                offset <= 8 -> minOf(offset + 2, masked.length)
                else        -> minOf(offset + 3, masked.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                var digits = 0
                for (i in 0 until minOf(offset, masked.length)) {
                    if (masked[i].isDigit()) digits++
                }
                return digits
            }
        }
        return TransformedText(AnnotatedString(masked), mapping)
    }
}

private class TelefoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        // Format: (XX) XXXXX-XXXX — celular 11 dígitos
        val masked = buildString {
            if (digits.isEmpty()) return@buildString
            append('(')
            digits.forEachIndexed { i, c ->
                if (i == 2) append(") ")
                if (i == 7) append('-')
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            // '(' antes do d[0]; ')' e ' ' entre d[1] e d[2]; '-' entre d[6] e d[7]
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 1 -> minOf(offset + 1, masked.length)
                offset <= 6 -> minOf(offset + 3, masked.length)
                else        -> minOf(offset + 4, masked.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                var digits = 0
                for (i in 0 until minOf(offset, masked.length)) {
                    if (masked[i].isDigit()) digits++
                }
                return digits
            }
        }
        return TransformedText(AnnotatedString(masked), mapping)
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpfInputScreen(
    viewModel: CpfInputViewModel,
    onBack: () -> Unit,
    onContinue: (clienteId: String) -> Unit,
) {
    val palette = LocalBoxPalette.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text("Identificação", fontFamily = FugazOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Surface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.contrast, titleContentColor = palette.onContrast),
            )
        },
        containerColor = palette.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (state.step) {
                CpfStep.ENTERING_CPF, CpfStep.LOADING ->
                    EnteringCpfContent(state, viewModel, onSkip = { onContinue("") })
                CpfStep.FOUND ->
                    FoundContent(state, onContinue = { onContinue(state.clienteEncontrado!!.id) }, onSkip = { onContinue("") })
                CpfStep.NOT_FOUND, CpfStep.REGISTERING ->
                    NotFoundContent(state, viewModel, onSuccess = onContinue, onSkip = { onContinue("") })
            }
        }
    }
}

@Composable
private fun EnteringCpfContent(
    state: CpfInputViewModel.UiState,
    viewModel: CpfInputViewModel,
    onSkip: () -> Unit,
) {
    val palette = LocalBoxPalette.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Sem card: no Figma o conteúdo fica direto sobre o fundo da tela e só o
    // campo de entrada é branco.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 28.dp, bottom = BliqDimens.BottomSafeGap),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            "DIGITE SEU CPF",
            fontFamily = FugazOne,
            fontSize = 28.sp,
            color = palette.onBackground,
        )
        Text(
            "Identifica sua conta para agilizar futuros atendimentos. Seus dados são tratados conforme a LGPD (Lei 13.709/2018).",
            fontFamily = Epilogue,
            fontSize = 16.sp,
            color = palette.onBackground,
            lineHeight = 23.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                "CPF",
                fontFamily = Epilogue,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = palette.onBackground,
                letterSpacing = 0.5.sp,
            )
            OutlinedTextField(
                value = state.cpf,
                onValueChange = { viewModel.updateCpf(it) },
                visualTransformation = CpfVisualTransformation(),
                placeholder = { Text("000.000.000-00", fontFamily = Epilogue, color = Tertiary) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = Epilogue,
                    fontSize = 20.sp,
                    color = palette.onSurface,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = palette.surface,
                    unfocusedContainerColor = palette.surface,
                    errorContainerColor     = palette.surface,
                    focusedBorderColor      = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor    = androidx.compose.ui.graphics.Color.Transparent,
                ),
                isError = state.cpf.length == 11 && !validarCpf(state.cpf),
            )
            if (state.cpf.length == 11 && !validarCpf(state.cpf)) {
                Text("CPF inválido", fontFamily = Epilogue, fontSize = 13.sp, color = palette.danger)
            }
        }

        if (state.step == CpfStep.LOADING) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    color = palette.onBackground,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text("Buscando cadastro...", fontFamily = Epilogue, fontSize = 15.sp, color = palette.onBackground)
            }
        }

        if (state.politicaCpf != PoliticaCpf.OBRIGATORIO) {
            Spacer(Modifier.height(8.dp))
            OutlineButton(label = "Pular identificação", onClick = onSkip)
        }
    }
}

@Composable
private fun FoundContent(
    state: CpfInputViewModel.UiState,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val palette = LocalBoxPalette.current
    val cliente = state.clienteEncontrado ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp, bottom = 24.dp + BliqDimens.BottomSafeGap),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SuccessLight, RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", fontSize = 36.sp, color = Success)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("BEM-VINDO DE VOLTA!", fontFamily = FugazOne, fontSize = 28.sp, color = palette.onBackground)
            Text(cliente.nome, fontFamily = Epilogue, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.onBackground)
            Text(maskCpf(cliente.cpf), fontFamily = Epilogue, fontSize = 16.sp, color = palette.onBackground.copy(alpha = 0.85f))
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(label = "Continuar", onClick = onContinue)
        OutlineButton(label = "Não sou eu", onClick = onSkip)
    }
}

@Composable
private fun NotFoundContent(
    state: CpfInputViewModel.UiState,
    viewModel: CpfInputViewModel,
    onSuccess: (clienteId: String) -> Unit,
    onSkip: () -> Unit,
) {
    val palette = LocalBoxPalette.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isRegistering = state.step == CpfStep.REGISTERING
    val canSubmit = state.nome.trim().length >= 2 &&
        state.telefone.length >= 10 &&
        state.aceitouTermos &&
        state.termos != null &&
        !isRegistering

    var showTerms by remember { mutableStateOf(false) }
    if (showTerms) TermsDialog(state.termos, onDismiss = { showTerms = false })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .padding(top = 32.dp, bottom = 24.dp + BliqDimens.BottomSafeGap),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("NOVO CADASTRO", fontFamily = FugazOne, fontSize = 28.sp, color = palette.onBackground)
        Text(
            "CPF não encontrado. Preencha seus dados para se cadastrar.",
            fontFamily = Epilogue,
            fontSize = 16.sp,
            color = palette.onBackground,
            lineHeight = 23.sp,
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledField(label = "CPF", value = maskCpf(state.cpf), enabled = false)
                LabeledField(
                    label = "NOME COMPLETO *",
                    value = state.nome,
                    onValueChange = viewModel::updateNome,
                    placeholder = "Seu nome",
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                )
                LabeledField(
                    label = "TELEFONE *",
                    value = state.telefone,
                    onValueChange = { viewModel.updateTelefone(it) },
                    placeholder = "(00) 00000-0000",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    visualTransformation = TelefoneVisualTransformation(),
                )
            }
        }

        ConsentsCard(
            state = state,
            onToggleTermos = viewModel::toggleAceitouTermos,
            onToggleMarketing = viewModel::toggleAceitaMarketing,
            onOpenTerms = { showTerms = true },
            onRetryTerms = viewModel::carregarTermos,
        )

        if (state.error != null) {
            Surface(color = ErrorSurface, shape = RoundedCornerShape(8.dp)) {
                Text(
                    state.error,
                    fontFamily = Epilogue,
                    color = Error,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        PrimaryButton(
            label = "Cadastrar e continuar",
            onClick = { viewModel.cadastrar(onSuccess) },
            enabled = canSubmit,
            loading = isRegistering,
        )
        if (state.politicaCpf != PoliticaCpf.OBRIGATORIO) {
            OutlineButton(label = "Pular identificação", onClick = onSkip, enabled = !isRegistering)
        }
    }
}

@Composable
private fun ConsentsCard(
    state: CpfInputViewModel.UiState,
    onToggleTermos: (Boolean) -> Unit,
    onToggleMarketing: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onRetryTerms: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                state.termosLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Carregando Termos de Uso...", fontFamily = Epilogue, fontSize = 14.sp, color = Secondary)
                    }
                }
                state.termosError != null -> {
                    Text(state.termosError, fontFamily = Epilogue, fontSize = 14.sp, color = Error)
                    OutlineButton(label = "Tentar novamente", onClick = onRetryTerms)
                }
                else -> {
                    ConsentRow(
                        checked = state.aceitouTermos,
                        onCheckedChange = onToggleTermos,
                        title = "Li e aceito os Termos de Uso e o Aviso de Privacidade da BLIQ.",
                        subtitle = "Aceite obrigatório para criar o cadastro e utilizar os serviços." +
                            (state.termos?.let { " Versão ${it.versao}." } ?: ""),
                        required = true,
                    )
                    TextButton(
                        onClick = onOpenTerms,
                        modifier = Modifier.padding(start = 24.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Ler termos completos", fontFamily = Epilogue, fontSize = 13.sp, color = Primary)
                    }

                    HorizontalDivider(color = PrimaryLight)

                    ConsentRow(
                        checked = state.aceitaMarketing,
                        onCheckedChange = onToggleMarketing,
                        title = "Quero receber promoções, ofertas e pesquisas da BLIQ por WhatsApp, SMS ou ligação.",
                        subtitle = "Opcional. Pode ser cancelado a qualquer momento, sem impedir o uso dos serviços.",
                        required = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String,
    required: Boolean,
) {
    val titleAnnotated = remember(title, required) {
        androidx.compose.ui.text.buildAnnotatedString {
            append(title)
            if (required) {
                append(' ')
                pushStyle(
                    androidx.compose.ui.text.SpanStyle(
                        color = Error,
                        fontSize = 12.sp,
                    )
                )
                append("(obrigatório)")
                pop()
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(checked) { detectTapGestures { onCheckedChange(!checked) } },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = Primary),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        ) {
            Text(
                text = titleAnnotated,
                fontFamily = Epilogue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                lineHeight = 20.sp,
            )
            Text(
                subtitle,
                fontFamily = Epilogue,
                fontSize = 12.sp,
                color = Secondary,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun TermsDialog(termos: br.com.bliqbrasil.totem.data.model.TermosAtual?, onDismiss: () -> Unit) {
    if (termos == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", fontFamily = Epilogue, color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        title = {
            Text(
                "Termos de Uso e Aviso de Privacidade",
                fontFamily = FugazOne,
                fontSize = 18.sp,
                color = OnSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Versão ${termos.versao} — publicado em ${termos.publicadoEm.take(10)}",
                    fontFamily = Epilogue,
                    fontSize = 12.sp,
                    color = Secondary,
                )
                Text(termos.conteudo, fontFamily = Epilogue, fontSize = 13.sp, color = OnSurface, lineHeight = 19.sp)
            }
        },
        containerColor = Surface,
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontFamily = Epilogue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Secondary,
            letterSpacing = 0.5.sp,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontFamily = Epilogue, color = Tertiary) },
            enabled = enabled,
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization,
                imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        )
    }
}
