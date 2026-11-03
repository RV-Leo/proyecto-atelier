package com.tecsup.atelier.data.repository

import com.tecsup.atelier.data.network.ClienteApiAtelier
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositorioCatalogoTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: RepositorioCatalogo

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val servicio = ClienteApiAtelier.crearServicio(
            urlBase = servidor.url("/api/v1/").toString()
        )
        repositorio = RepositorioCatalogoImpl(servicio)
    }

    @After
    fun cerrar() {
        servidor.shutdown()
    }

    @Test
    fun `lista prendas y las convierte al modelo de la app`() = runBlocking {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "items": [
                        {
                          "id": 28,
                          "nombre": "Blusa lino natural",
                          "slug": "blusa-lino-natural",
                          "tipoPrenda": "blusa",
                          "estilo": "casual",
                          "colores": "natural",
                          "tela": "lino",
                          "descripcion": "Blusa fresca de lino natural.",
                          "estado": "ANALIZADA"
                        }
                      ],
                      "total": 1,
                      "pagina": 1,
                      "tamano": 20,
                      "totalPaginas": 1,
                      "primera": true,
                      "ultima": true
                    }
                    """.trimIndent()
                )
        )

        val resultado = repositorio.listarPrendas()

        assertTrue(resultado.isSuccess)
        val pagina = resultado.getOrThrow()
        assertEquals(1, pagina.total)
        assertEquals(1, pagina.totalPaginas)
        assertEquals(1, pagina.items.size)

        val prenda = pagina.items.first()
        assertEquals("28", prenda.id)
        assertEquals("Blusa lino natural", prenda.nombre)
        assertEquals("blusa-lino-natural", prenda.slug)
        assertEquals("blusa", prenda.idCategoria)
        assertEquals("Blusa", prenda.nombreCategoria)
        assertEquals("lino", prenda.tela)
        assertEquals("casual", prenda.estilo)
        assertEquals("natural", prenda.colores)
        assertEquals("ANALIZADA", prenda.estado)
        assertEquals("Analizada", prenda.estadoLegible)
        assertEquals(0.0, prenda.precio, 0.0)
        assertEquals(0.0, prenda.calificacion, 0.0)
        assertEquals(0, prenda.cantidadResenas)
        assertEquals(0xFFD9CBB3, prenda.colorHex)

        val peticion = servidor.takeRequest()
        assertTrue(peticion.path!!.startsWith("/api/v1/prendas"))
        assertEquals(
            Credentials.basic(ClienteApiAtelier.USUARIO, ClienteApiAtelier.CLAVE),
            peticion.getHeader("Authorization")
        )
    }

    @Test
    fun `devuelve fallo cuando la API responde con error`() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(500))

        val resultado = repositorio.listarPrendas()

        assertTrue(resultado.isFailure)
        assertNotNull(resultado.exceptionOrNull())
    }

    @Test
    fun `obtiene el detalle por slug`() = runBlocking {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 65,
                      "nombre": "Vestido de verano",
                      "slug": "vestido-de-verano",
                      "tipoPrenda": "vestido",
                      "estilo": "playero",
                      "colores": "beige",
                      "tela": "viscosa",
                      "descripcion": "Vestido ligero.",
                      "estado": "GENERADA"
                    }
                    """.trimIndent()
                )
        )

        val resultado = repositorio.obtenerPorSlug("vestido-de-verano")

        assertTrue(resultado.isSuccess)
        val prenda = resultado.getOrNull()
        assertNotNull(prenda)
        assertEquals("Vestido de verano", prenda!!.nombre)
        assertEquals("Generada", prenda.estadoLegible)

        val peticion = servidor.takeRequest()
        assertEquals("/api/v1/prendas/slug/vestido-de-verano", peticion.path)
    }

    @Test
    fun `un slug inexistente devuelve null y no un error`() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(404))

        val resultado = repositorio.obtenerPorSlug("no-existe")

        assertTrue(resultado.isSuccess)
        assertNull(resultado.getOrNull())
    }
}
