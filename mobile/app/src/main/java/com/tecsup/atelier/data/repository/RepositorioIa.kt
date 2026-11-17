package com.tecsup.atelier.data.repository

import com.tecsup.atelier.data.modelo.MensajeIaDto
import com.tecsup.atelier.data.modelo.SolicitudIaDto
import com.tecsup.atelier.data.network.ClienteApiIa
import com.tecsup.atelier.data.network.ServicioApiIa
import java.io.IOException

interface RepositorioIa {
    suspend fun consultarIa(
        prompt: String,
        historialMensajes: List<MensajeIaDto> = emptyList()
    ): Result<String>
}

class RepositorioIaImpl(
    private val servicioApiIa: ServicioApiIa = ClienteApiIa.servicio
) : RepositorioIa {

    override suspend fun consultarIa(
        prompt: String,
        historialMensajes: List<MensajeIaDto>
    ): Result<String> {
        return try {
            val mensajesConPrompt = historialMensajes + MensajeIaDto(rol = "user", contenido = prompt)
            val solicitud = SolicitudIaDto(
                prompt = prompt,
                mensajes = mensajesConPrompt
            )

            val respuesta = servicioApiIa.generarRespuestaIa(solicitud)

            if (respuesta.isSuccessful) {
                val cuerpoRespuesta = respuesta.body()
                val textoRespuesta = cuerpoRespuesta?.respuestaTexto
                    ?: cuerpoRespuesta?.opciones?.firstOrNull()?.mensaje?.contenido
                    ?: cuerpoRespuesta?.opciones?.firstOrNull()?.texto
                    ?: "Respuesta recibida correctamente del servidor de IA."

                Result.success(textoRespuesta)
            } else {
                val codigoError = respuesta.code()
                Result.failure(IOException("Error del servidor de IA (Código $codigoError). Verifica la URL y tu API Key."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
