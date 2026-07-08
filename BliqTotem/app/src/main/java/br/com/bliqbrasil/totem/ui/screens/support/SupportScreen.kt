package br.com.bliqbrasil.totem.ui.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.bliqbrasil.totem.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Fale Conosco",
                        fontFamily = FugazOne,
                        fontSize = 20.sp,
                        color = OnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = OnSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Precisa de ajuda? Entre em contato com nossa equipe de suporte.",
                fontFamily = Epilogue,
                fontSize = 15.sp,
                color = Secondary,
            )

            ContactRow(label = "E-mail", value = "angelo@bliqbrasil.com.br")
            ContactRow(label = "Telefone / WhatsApp", value = "(48) 98821-6049")

            HorizontalDivider(color = Divider)

            Text(
                text = "Horário de atendimento",
                fontFamily = FugazOne,
                fontSize = 16.sp,
                color = OnSurface,
            )
            Text(
                text = "Segunda a sexta, das 9h às 18h.",
                fontFamily = Epilogue,
                fontSize = 14.sp,
                color = Secondary,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContactRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontFamily = Epilogue,
            fontSize = 12.sp,
            color = Tertiary,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            fontFamily = Epilogue,
            fontSize = 15.sp,
            color = OnSurface,
        )
    }
}
