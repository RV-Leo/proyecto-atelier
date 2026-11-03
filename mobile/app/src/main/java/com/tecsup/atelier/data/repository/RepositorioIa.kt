package com.tecsup.atelier.data.repository

import com.tecsup.atelier.data.modelo.MensajeIaDto
import com.tecsup.atelier.data.modelo.SolicitudIaDto
import com.tecsup.atelier.data.network.ClienteApiIa
import com.tecsup.atelier.data.network.ServicioApiIa
import kotlinx.coroutines.CancellationException
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
            val solicitud = SolicitudIaDto(mensajes = mensajesConPrompt)

            val respuesta = servicioApiIa.generarRespuestaIa(solicitud)

            if (respuesta.isSuccessful) {
                val textoRespuesta = respuesta.body()?.respuestaTexto
                if (textoRespuesta.isNullOrBlank()) {
                    Result.failure(IOException("El puente de IA devolvió una respuesta vacía."))
                } else {
                    Result.success(textoRespuesta)
                }
            } else {
                val codigoError = respuesta.code()
                Result.failure(IOException("Error del puente de IA (código $codigoError)."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: RuntimeException) {
            Result.failure(e)
        }
    }
}
