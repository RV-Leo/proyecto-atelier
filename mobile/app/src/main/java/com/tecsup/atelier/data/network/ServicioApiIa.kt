package com.tecsup.atelier.data.network

import com.tecsup.atelier.data.modelo.RespuestaIaDto
import com.tecsup.atelier.data.modelo.SolicitudIaDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Contrato Retrofit del puente de IA común de Atelier. */
interface ServicioApiIa {

    @POST("api/v1/ia/chat")
    suspend fun generarRespuestaIa(
        @Body solicitud: SolicitudIaDto
    ): Response<RespuestaIaDto>
}
