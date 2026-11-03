package com.tecsup.atelier.data.repository

import com.tecsup.atelier.data.modelo.MensajeIaDto
import com.tecsup.atelier.data.modelo.RespuestaIaDto
import com.tecsup.atelier.data.modelo.SolicitudIaDto
import com.tecsup.atelier.data.network.ServicioApiIa
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class RepositorioIaTests {

    @Test
    fun enviaHistorialYConsultaAlBackend() = runBlocking {
        val servicio = ServicioIaFalso()
        val repositorio = RepositorioIaImpl(servicio)
        val historial = listOf(MensajeIaDto("assistant", "Hola"))

        val resultado = repositorio.consultarIa("Sugiere un outfit", historial)

        assertEquals("Respuesta generada", resultado.getOrThrow())
        assertEquals(
            listOf(
                MensajeIaDto("assistant", "Hola"),
                MensajeIaDto("user", "Sugiere un outfit")
            ),
            servicio.solicitudRecibida?.mensajes
        )
    }

    private class ServicioIaFalso : ServicioApiIa {
        var solicitudRecibida: SolicitudIaDto? = null

        override suspend fun generarRespuestaIa(
            solicitud: SolicitudIaDto
        ): Response<RespuestaIaDto> {
            solicitudRecibida = solicitud
            return Response.success(RespuestaIaDto("Respuesta generada"))
        }
    }
}
