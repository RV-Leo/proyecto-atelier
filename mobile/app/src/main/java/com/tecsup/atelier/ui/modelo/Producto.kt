package com.tecsup.atelier.ui.modelo

data class Categoria(
    val id: String,
    val nombre: String
)

data class Producto(
    val id: String,
    val nombre: String,
    val idCategoria: String,
    val nombreCategoria: String,
    val precio: Double,
    val precioOriginal: Double? = null,
    val calificacion: Double,
    val cantidadResenas: Int,
    val descripcion: String,
    val esPopular: Boolean = false,
    val esNuevo: Boolean = false,
    val esFavorito: Boolean = false,
    val colorHex: Long = 0xFF2C3E50
)
