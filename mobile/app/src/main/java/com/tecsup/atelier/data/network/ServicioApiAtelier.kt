package com.tecsup.atelier.data.network

import com.tecsup.atelier.data.modelo.PaginaPrendasDto
import com.tecsup.atelier.data.modelo.PrendaDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints del catálogo del backend Spring Boot.
 *
 * `pagina` es base 1. Si se omite, la API devuelve todas las prendas.
 */
interface ServicioApiAtelier {

    @GET("prendas")
    suspend fun listarPrendas(
        @Query("pagina") pagina: Int = 1,
        @Query("tamano") tamano: Int = 20,
        @Query("q") q: String? = null,
        @Query("tipo") tipo: String? = null,
        @Query("estilo") estilo: String? = null,
        @Query("estado") estado: String? = null
    ): Response<PaginaPrendasDto>

    @GET("prendas/slug/{slug}")
    suspend fun obtenerPrendaPorSlug(
        @Path("slug") slug: String
    ): Response<PrendaDto>
}
