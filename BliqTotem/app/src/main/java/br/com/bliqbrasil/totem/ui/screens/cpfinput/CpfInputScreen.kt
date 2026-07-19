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
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identificação", fontFamily = FugazOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Surface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Surface),
            )
        },
        containerColor = Background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (state.step) {
                CpfStep.CONSENT ->
                    ConsentContent(
                        onAccept = viewModel::aceitarConsentimento,
                        onDecline = { onContinue("") },
                    )
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
private fun ConsentContent(onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🔒", fontSize = 36.sp)
        Text(
            "Uso de dados",
            fontFamily = FugazOne,
            fontSize = 22.sp,
            color = OnSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            "Para agilizar futuras visitas, registraremos seu CPF, nome e telefone. " +
                "Dados protegidos conforme a LGPD (Lei 13.709/2018).",
            fontFamily = Epilogue,
            fontSize = 14.sp,
            color = Secondary,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )
        PrimaryButton(label = "Autorizar e continuar", onClick = onAccept)
        OutlineButton(label = "Continuar sem identificação", onClick = onDecline)
    }
}

@Composable
private fun EnteringCpfContent(
    state: CpfInputViewModel.UiState,
    viewModel: CpfInputViewModel,
    onSkip: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Digite seu CPF", fontFamily = FugazOne, fontSize = 28.sp, color = OnSurface)
        Text(
            "Identificamos sua conta para agilizar o atendimento",
            fontFamily = Epilogue,
            fontSize = 15.sp,
            color = Secondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CPF",
                    fontFamily = Epilogue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Secondary,
                    letterSpacing = 0.5.sp,
                )
                OutlinedTextField(
                    value = state.cpf,
                    onValueChange = { viewModel.updateCpf(it) },
                    visualTransformation = CpfVisualTransformation(),
                    placeholder = { Text("000.000.000-00", fontFamily = Epilogue, color = Tertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(10.dp),
                    isError = state.cpf.length == 11 && !validarCpf(state.cpf),
                    supportingText = if (state.cpf.length == 11 && !validarCpf(state.cpf)) {
                        { Text("CPF inválido", fontFamily = Epilogue, color = Error) }
                    } else null,
                )
            }
        }

        if (state.step == CpfStep.LOADING) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Buscando cadastro...", fontFamily = Epilogue, fontSize = 14.sp, color = Secondary)
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlineButton(label = "Pular identificação", onClick = onSkip)
    }
}

@Composable
private fun FoundContent(
    state: CpfInputViewModel.UiState,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val cliente = state.clienteEncontrado ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp, bottom = 24.dp),
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
            Text("Bem-vindo de volta!", fontFamily = FugazOne, fontSize = 26.sp, color = OnSurface)
            Text(cliente.nome, fontFamily = Epilogue, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            Text(maskCpf(cliente.cpf), fontFamily = Epilogue, fontSize = 15.sp, color = Secondary)
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
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isRegistering = state.step == CpfStep.REGISTERING
    val canSubmit = state.nome.trim().length >= 2 && state.telefone.length >= 10 && !isRegistering

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Novo cadastro", fontFamily = FugazOne, fontSize = 26.sp, color = OnSurface)
        Text(
            "CPF não encontrado. Preencha seus dados para se cadastrar.",
            fontFamily = Epilogue,
            fontSize = 14.sp,
            color = Secondary,
            lineHeight = 21.sp,
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // CPF desabilitado: sem cursor, pode usar valor mascarado diretamente
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
        OutlineButton(label = "Pular identificação", onClick = onSkip, enabled = !isRegistering)
    }
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
