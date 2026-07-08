package br.com.bliqbrasil.totem.ui.screens.activation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.bliqbrasil.totem.ui.components.PrimaryButton
import br.com.bliqbrasil.totem.ui.theme.*

@Composable
fun ActivationScreen(
    viewModel: ActivationViewModel,
    onActivated: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.activated) {
        if (state.activated) onActivated()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🚗", fontSize = 64.sp)
                Text(
                    text = "Bliq Totem",
                    fontSize = 30.sp,
                    fontFamily = FugazOne,
                    color = OnSurface,
                )
                Text(
                    text = "Digite o código de ativação\ngerado no painel da franqueadora",
                    fontSize = 15.sp,
                    fontFamily = Epilogue,
                    color = Secondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }

            // Card
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "CÓDIGO DE ATIVAÇÃO",
                        fontSize = 13.sp,
                        fontFamily = Epilogue,
                        fontWeight = FontWeight.SemiBold,
                        color = Secondary,
                        letterSpacing = 0.5.sp,
                    )
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = { if (it.length <= 8) viewModel.updateCode(it) },
                        placeholder = {
                            Text(
                                "XXXXXXXX",
                                fontFamily = Epilogue,
                                color = Tertiary,
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = Epilogue,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = 6.sp,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboard?.hide()
                            viewModel.activate()
                        }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Text(
                        text = "8 caracteres — válido por 2 horas",
                        fontSize = 12.sp,
                        fontFamily = Epilogue,
                        color = Tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.error != null) {
                        Surface(
                            color = ErrorSurface,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = state.error!!,
                                fontFamily = Epilogue,
                                color = Error,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            )
                        }
                    }
                }
            }

            PrimaryButton(
                label = "Ativar terminal",
                onClick = {
                    keyboard?.hide()
                    viewModel.activate()
                },
                enabled = state.code.trim().length == 8,
                loading = state.loading,
            )

            Text(
                text = "Peça o código ao gestor da franqueadora",
                fontSize = 13.sp,
                fontFamily = Epilogue,
                color = Tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
