package com.tecsup.atelier.ui.modelo

data class Categoria(
    val id: String,
    val nombre: String
)

/**
 * Modelo que usan las pantallas. Se llena desde la API del backend:
 * los campos que la API todavía no expone (precio, reseñas, stock) quedan
 * en cero y la interfaz los oculta en lugar de inventarlos.
 */
data class Producto(
    val id: String,
    val nombre: String,
    val idCategoria: String = "",
    val nombreCategoria: String = "",
    val precio: Double = 0.0,
    val precioOriginal: Double? = null,
    val calificacion: Double = 0.0,
    val cantidadResenas: Int = 0,
    val descripcion: String = "",
    val esPopular: Boolean = false,
    val esNuevo: Boolean = false,
    val esFavorito: Boolean = false,
    val colorHex: Long = 0xFF2C3E50,
    val slug: String = "",
    val tela: String = "",
    val estilo: String = "",
    val colores: String = "",
    val estado: String = ""
) {
    val tienePrecio: Boolean
        get() = precio > 0.0

    val estadoLegible: String
        get() = when (estado.uppercase()) {
            "BORRADOR" -> "Borrador"
            "GENERANDO" -> "En generación"
            "GENERADA" -> "Generada"
            "ANALIZADA" -> "Analizada"
            else -> estado.ifBlank { "—" }
        }
}
