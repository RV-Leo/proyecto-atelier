package com.tecsup.atelier.ui.screens.ia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MensajeChat(
    val remitente: String,
    val texto: String,
    val esUsuario: Boolean
)

@Composable
fun AiAssistantScreen(modifier: Modifier = Modifier) {
    var mensajes by remember {
        mutableStateOf(
            listOf(
                MensajeChat(
                    remitente = "Atelier AI",
                    texto = "¡Hola! 👋 Soy Atelier IA. ¿En qué puedo ayudarte hoy? Puedo recomendarte outfits o generar tus propios diseños.",
                    esUsuario = false
                )
            )
        )
    }
    var textoEntrada by remember { mutableStateOf("") }

    val sugerenciasConsulta = listOf(
        "✨ Armar un outfit casual con polo blanco",
        "📏 Quiero generar un diseño propio para unas casacas",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Encabezado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = "Atelier IA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Asesoría de estilo personalizada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sugerencias rápidas
        Text(
            text = "Sugerencias rápidas:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            sugerenciasConsulta.forEach { sugerencia ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val mensajeUsuario = MensajeChat("Usuario", sugerencia, true)
                            val respuestaIA = MensajeChat(
                                "Atelier AI",
                                "¡Excelente elección! Para un outfit óptimo con tus prendas textiles, te sugiero combinar un Polo Pima Minimalist con el Pantalón Jean Slim Fit y calzado casual en tonos neutros.",
                                false
                            )
                            mensajes = mensajes + mensajeUsuario + respuestaIA
                        }
                ) {
                    Text(
                        text = sugerencia,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de mensajes de chat
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(mensajes) { mensaje ->
                BurbujaMensajeChat(mensaje = mensaje)
            }
        }

        // Campo de entrada de texto
        OutlinedTextField(
            value = textoEntrada,
            onValueChange = { textoEntrada = it },
            placeholder = { Text("Escribe tu consulta a la IA...") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BurbujaMensajeChat(mensaje: MensajeChat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mensaje.esUsuario) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mensaje.esUsuario) 16.dp else 4.dp,
                bottomEnd = if (mensaje.esUsuario) 4.dp else 16.dp
            ),
            color = if (mensaje.esUsuario) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (mensaje.esUsuario) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = mensaje.texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
