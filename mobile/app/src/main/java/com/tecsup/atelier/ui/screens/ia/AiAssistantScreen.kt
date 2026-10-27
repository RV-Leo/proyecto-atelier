package com.tecsup.atelier.ui.screens.ia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.atelier.R
import com.tecsup.atelier.ui.theme.AtelierTheme

data class MensajeChat(
    val remitente: String,
    val texto: String,
    val esUsuario: Boolean,
    val outfitRecomendado: OutfitRecomendado? = null
)

data class OutfitRecomendado(
    val titulo: String,
    val prendasSummary: String,
    val precioTotal: Double,
    val matchScore: String,
    val colorHex: Long
)

@Composable
fun AiAssistantScreen(modifier: Modifier = Modifier) {
    var mensajes by remember {
        mutableStateOf(
            listOf(
                MensajeChat(
                    remitente = "Atelier AI",
                    texto = "¡Hola, Cliente! 👋 Soy tu estilista de moda IA. ¿Qué ocasión tienes hoy? Puedo armarte un outfit completo con combinaciones exclusivas de nuestra colección.",
                    esUsuario = false
                ),
                MensajeChat(
                    remitente = "Atelier AI",
                    texto = "Aquí tienes una recomendación destacada basada en las tendencias de la temporada:",
                    esUsuario = false,
                    outfitRecomendado = OutfitRecomendado(
                        titulo = "Outfit Urban Minimalist",
                        prendasSummary = "Polo Pima Premium Azul + Jean Slim Fit Denim",
                        precioTotal = 199.80,
                        matchScore = "98% Match",
                        colorHex = 0xFF2C3E50
                    )
                )
            )
        )
    }
    var textoEntrada by remember { mutableStateOf("") }

    val sugerenciasConsulta = listOf(
        "Armar outfit casual de fin de semana",
        "¿Qué polo combina con Jean Slim Fit?",
        "Guía de tallas en algodón Pima"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ai_assistant),
                        contentDescription = "Asistente IA",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Asistente de Estilo IA ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Asesoría personalizada en moda Atelier",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sugerencias Rápidas
        Text(
            text = "Sugerencias de estilo:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sugerenciasConsulta.forEach { sugerencia ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            val mensajeUsuario = MensajeChat("Usuario", sugerencia, true)
                            val respuestaIA = MensajeChat(
                                "Atelier AI",
                                "¡Excelente elección! Te recomiendo este outfit completo:",
                                false,
                                OutfitRecomendado(
                                    titulo = "Outfit Elegante Casual",
                                    prendasSummary = "Camisa Lino Blanca + Pantalón Chino Beige",
                                    precioTotal = 234.80,
                                    matchScore = "96% Match",
                                    colorHex = 0xFFD7CCC8
                                )
                            )
                            mensajes = mensajes + mensajeUsuario + respuestaIA
                        }
                ) {
                    Text(
                        text = "✨ $sugerencia",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de Mensajes del Chat
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(mensajes) { mensaje ->
                BurbujaMensajeChat(mensaje = mensaje)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Campo de entrada de texto
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textoEntrada,
                onValueChange = { textoEntrada = it },
                placeholder = { Text("Escribe tu consulta de moda...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(50.dp)
                    .clickable {
                        if (textoEntrada.isNotBlank()) {
                            val userMsg = MensajeChat("Usuario", textoEntrada, true)
                            val aiMsg = MensajeChat(
                                "Atelier AI",
                                "Para tu consulta \"$textoEntrada\", te recomiendo combinar tus prendas favoritas en tonos neutros para mantener la elegancia.",
                                false
                            )
                            mensajes = mensajes + userMsg + aiMsg
                            textoEntrada = ""
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Enviar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BurbujaMensajeChat(mensaje: MensajeChat) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mensaje.esUsuario) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (mensaje.esUsuario) 18.dp else 4.dp,
                bottomEnd = if (mensaje.esUsuario) 4.dp else 18.dp
            ),
            color = if (mensaje.esUsuario) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (mensaje.esUsuario) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = mensaje.texto,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Tarjeta de Outfit Recomendado
                mensaje.outfitRecomendado?.let { outfit ->
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(outfit.colorHex))
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = outfit.matchScore,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = outfit.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = outfit.prendasSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "S/ ${"%.2f".format(outfit.precioTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = { },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Añadir Outfit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiAssistantScreenPreview() {
    AtelierTheme {
        AiAssistantScreen()
    }
}
