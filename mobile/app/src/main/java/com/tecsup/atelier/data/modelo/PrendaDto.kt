package com.tecsup.atelier.data.modelo

import com.google.gson.annotations.SerializedName

/**
 * Prenda tal como la devuelve `GET /api/v1/prendas` del backend.
 * La API no expone precio, imagen, stock ni calificación.
 */
data class PrendaDto(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("nombre")
    val nombre: String = "",
    @SerializedName("slug")
    val slug: String = "",
    @SerializedName("tipoPrenda")
    val tipoPrenda: String = "",
    @SerializedName("estilo")
    val estilo: String = "",
    @SerializedName("colores")
    val colores: String = "",
    @SerializedName("tela")
    val tela: String = "",
    @SerializedName("descripcion")
    val descripcion: String = "",
    @SerializedName("estado")
    val estado: String = "",
    @SerializedName("creadoEn")
    val creadoEn: String? = null,
    @SerializedName("actualizadoEn")
    val actualizadoEn: String? = null
)

/**
 * Respuesta paginada de la API. `pagina` es base 1.
 */
data class PaginaPrendasDto(
    @SerializedName("items")
    val items: List<PrendaDto> = emptyList(),
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("pagina")
    val pagina: Int = 1,
    @SerializedName("tamano")
    val tamano: Int = 20,
    @SerializedName("totalPaginas")
    val totalPaginas: Int = 1,
    @SerializedName("primera")
    val primera: Boolean = true,
    @SerializedName("ultima")
    val ultima: Boolean = true
)
