package com.tecsup.atelier.data.network

import com.tecsup.atelier.data.modelo.RespuestaIaDto
import com.tecsup.atelier.data.modelo.SolicitudIaDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interfaz Retrofit para la comunicación con la API del modelo de IA en la nube o servidor dedicado.
 * 
 * TODO: Modifica el endpoint ("api/v1/chat" o "v1/chat/completions") según la ruta exacta de tu servidor/proveedor.
 */
interface ServicioApiIa {

    @POST("api/v1/chat/completions")
    suspend fun generarRespuestaIa(
        @Body solicitud: SolicitudIaDto
    ): Response<RespuestaIaDto>
}
