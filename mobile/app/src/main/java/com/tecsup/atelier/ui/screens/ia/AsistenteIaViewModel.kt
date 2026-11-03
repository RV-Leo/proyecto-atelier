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
        if (promptTexto.isBlank() || _uiState.value.cargando) return

        val historialDto = _uiState.value.mensajes
            .takeLast(19)
            .map {
                MensajeIaDto(
                    rol = if (it.esUsuario) "user" else "assistant",
                    contenido = it.texto
                )
            }

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
            }.onFailure {
                _uiState.update { estadoActual ->
                    estadoActual.copy(
                        cargando = false,
                        errorMensaje = "No fue posible conectar con la IA. Revisa tu conexión y que el servidor esté disponible; puedes intentarlo de nuevo."
                    )
                }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(errorMensaje = null) }
    }
}
