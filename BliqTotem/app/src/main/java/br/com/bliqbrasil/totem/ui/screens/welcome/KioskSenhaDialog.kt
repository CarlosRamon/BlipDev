package br.com.bliqbrasil.totem.ui.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.bliqbrasil.totem.ui.theme.Epilogue
import br.com.bliqbrasil.totem.ui.theme.Error
import br.com.bliqbrasil.totem.ui.theme.FugazOne

/**
 * Pedido de senha do operador para ligar/desligar o bloqueio dos botões do
 * sistema. Aberto segurando o "Toque para iniciar" por 10 segundos.
 */
@Composable
fun KioskSenhaDialog(
    ativo: Boolean,
    bloqueioCompleto: Boolean,
    senhaConfere: (String) -> Boolean,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit,
) {
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(false) }
    val foco = remember { FocusRequester() }

    LaunchedEffect(Unit) { foco.requestFocus() }

    val confirmar = {
        if (senhaConfere(senha)) onConfirmar() else { erro = true; senha = "" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (ativo) "Liberar botões do sistema?" else "Bloquear botões do sistema?",
                fontFamily = FugazOne,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (ativo) "O totem volta a permitir sair do app pelos botões Home e Recentes."
                    else "Home e Recentes ficam desativados e o totem não sai do app.",
                    fontFamily = Epilogue,
                )
                if (!ativo && !bloqueioCompleto) {
                    Text(
                        "Este terminal não está configurado como device owner: o Android vai " +
                            "pedir confirmação, e segurar Voltar junto com Início (ou Recentes) destrava.",
                        fontFamily = Epilogue,
                        fontSize = 13.sp,
                    )
                }
                OutlinedTextField(
                    value = senha,
                    onValueChange = { novo ->
                        senha = novo.filter(Char::isDigit).take(8)
                        erro = false
                    },
                    label = { Text("Senha", fontFamily = Epilogue) },
                    singleLine = true,
                    isError = erro,
                    supportingText = if (erro) {
                        { Text("Senha incorreta", fontFamily = Epilogue) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.focusRequester(foco),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirmar, enabled = senha.isNotEmpty()) {
                Text(
                    if (ativo) "Liberar" else "Bloquear",
                    color = if (ativo) Error else Color.Unspecified,
                    fontFamily = Epilogue,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", fontFamily = Epilogue) }
        },
    )
}
