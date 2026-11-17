package com.tecsup.atelier.ui.screens.ia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.atelier.data.modelo.MensajeIaDto
import com.tecsup.atelier.data.repository.RepositorioIa
import com.tecsup.atelier.data.repository.RepositorioIaImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoIaUi(
    val cargando: Boolean = false,
    val errorMensaje: String? = null,
    val mensajes: List<MensajeChat> = listOf(
        MensajeChat(
            remitente = "Atelier AI",
            texto = "¡Hola, Cliente! 👋 Soy tu estilista de moda IA. ¿En qué puedo ayudarte hoy? Puedo recomendarte outfits, guías de tallas en algodón Pima y sugerirte combinaciones exclusivas.",
            esUsuario = false
        )
    )
)

class AsistenteIaViewModel(
    private val repositorioIa: RepositorioIa = RepositorioIaImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstadoIaUi())
    val uiState: StateFlow<EstadoIaUi> = _uiState.asStateFlow()

    fun enviarConsulta(promptTexto: String) {
        if (promptTexto.isBlank()) return

        val mensajeUsuario = MensajeChat(
            remitente = "Usuario",
            texto = promptTexto,
            esUsuario = true
        )

        // Agregar mensaje del usuario inmediatamente al chat
        _uiState.update { estadoActual ->
            estadoActual.copy(
                cargando = true,
                errorMensaje = null,
                mensajes = estadoActual.mensajes + mensajeUsuario
            )
        }

        viewModelScope.launch {
            // Mapear historial al formato DTO
            val historialDto = _uiState.value.mensajes.map {
                MensajeIaDto(
                    rol = if (it.esUsuario) "user" else "assistant",
                    contenido = it.texto
                )
            }

            val resultado = repositorioIa.consultarIa(promptTexto, historialDto)

            resultado.onSuccess { respuestaTexto ->
                val mensajeIa = MensajeChat(
                    remitente = "Atelier AI",
                    texto = respuestaTexto,
                    esUsuario = false
                )
                _uiState.update { estadoActual ->
                    estadoActual.copy(
                        cargando = false,
                        mensajes = estadoActual.mensajes + mensajeIa
                    )
                }
            }.onFailure { excepcion ->
                // Si la API no está configurada aún o falla la conexión, mostramos respuesta simulada
                val respuestaFallback = MensajeChat(
                    remitente = "Atelier AI",
                    texto = "Para tu consulta \"$promptTexto\", te sugiero combinar un Polo Pima Minimalist con Jeans Slim Fit. (Estructura API lista para ingresar tu servidor/API Key).",
                    esUsuario = false,
                    outfitRecomendado = OutfitRecomendado(
                        titulo = "Sugerencia de Estilo Atelier",
                        prendasSummary = "Polo Pima Premium Azul + Jean Slim Fit Denim",
                        precioTotal = 199.80,
                        matchScore = "98% Match",
                        colorHex = 0xFF2C3E50
                    )
                )

                _uiState.update { estadoActual ->
                    estadoActual.copy(
                        cargando = false,
                        errorMensaje = "Conexión pendiente: Configura tu URL Base y API Key en ClienteApiIa.kt",
                        mensajes = estadoActual.mensajes + respuestaFallback
                    )
                }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(errorMensaje = null) }
    }
}
