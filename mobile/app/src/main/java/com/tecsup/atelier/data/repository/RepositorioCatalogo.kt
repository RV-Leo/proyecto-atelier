package com.tecsup.atelier.data.repository

import com.tecsup.atelier.data.modelo.PaginaPrendasDto
import com.tecsup.atelier.data.modelo.PrendaDto
import com.tecsup.atelier.data.network.ClienteApiAtelier
import com.tecsup.atelier.data.network.ServicioApiAtelier
import com.tecsup.atelier.ui.modelo.Producto
import com.tecsup.atelier.ui.modelo.colorHexDesde
import java.io.IOException

data class PaginaPrendas(
    val items: List<Producto>,
    val total: Int,
    val pagina: Int,
    val totalPaginas: Int
)

interface RepositorioCatalogo {
    suspend fun listarPrendas(
        pagina: Int = 1,
        tamano: Int = 20,
        q: String? = null,
        tipo: String? = null,
        estilo: String? = null
    ): Result<PaginaPrendas>

    suspend fun obtenerPorSlug(slug: String): Result<Producto?>
}

/**
 * Única puerta de entrada del catálogo móvil a la API.
 * Convierte los DTO del backend al modelo que usan las pantallas.
 */
class RepositorioCatalogoImpl(
    private val servicio: ServicioApiAtelier = ClienteApiAtelier.servicio
) : RepositorioCatalogo {

    override suspend fun listarPrendas(
        pagina: Int,
        tamano: Int,
        q: String?,
        tipo: String?,
        estilo: String?
    ): Result<PaginaPrendas> = try {
        val respuesta = servicio.listarPrendas(pagina, tamano, q, tipo, estilo)
        if (!respuesta.isSuccessful) {
            Result.failure(IOException("La API respondió ${respuesta.code()} al pedir las prendas."))
        } else {
            val cuerpo = respuesta.body()
                ?: throw IOException("La API devolvió una respuesta vacía.")
            Result.success(cuerpo.aPagina())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun obtenerPorSlug(slug: String): Result<Producto?> = try {
        val respuesta = servicio.obtenerPrendaPorSlug(slug)
        when {
            respuesta.code() == 404 -> Result.success(null)
            respuesta.isSuccessful -> Result.success(respuesta.body()?.aProducto())
            else -> Result.failure(
                IOException("La API respondió ${respuesta.code()} al pedir la prenda '$slug'.")
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun PaginaPrendasDto.aPagina(): PaginaPrendas = PaginaPrendas(
    items = items.map { it.aProducto() },
    total = total,
    pagina = pagina,
    totalPaginas = totalPaginas
)

fun PrendaDto.aProducto(): Producto {
    val tipo = tipoPrenda.trim()
    val categoria = tipo.replaceFirstChar { it.uppercase() }.ifBlank { "Prenda" }
    return Producto(
        id = id.toString(),
        nombre = nombre.ifBlank { "Prenda sin nombre" },
        idCategoria = tipo.lowercase().replace(" ", "-").ifBlank { "prenda" },
        nombreCategoria = categoria,
        descripcion = descripcion,
        colorHex = colorHexDesde(colores),
        slug = slug,
        tela = tela,
        estilo = estilo,
        colores = colores,
        estado = estado
    )
}
