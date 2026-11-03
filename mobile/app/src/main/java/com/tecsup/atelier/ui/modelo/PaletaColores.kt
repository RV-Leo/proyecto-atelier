package com.tecsup.atelier.ui.modelo

/**
 * Traduce los nombres de color que devuelve la API a un color de la interfaz.
 * No agrega datos de negocio: si el nombre no se reconoce se usa el color base.
 */
private val PALETA_COLORES = listOf(
    "blanco" to 0xFFF2F0EB,
    "negro" to 0xFF1E1E1E,
    "beige" to 0xFFD7CCC8,
    "marron" to 0xFF8D6E63,
    "azul" to 0xFF1B3B6F,
    "celeste" to 0xFF90CAF9,
    "verde" to 0xFF2E7D32,
    "rojo" to 0xFFC62828,
    "rosa" to 0xFFE91E63,
    "morado" to 0xFF8E24AA,
    "gris" to 0xFF78909C,
    "amarillo" to 0xFFF9A825,
    "naranja" to 0xFFEF6C00,
    "natural" to 0xFFD9CBB3,
    "lino" to 0xFFD9CBB3,
    "viscosa" to 0xFFB0BEC5
)

private fun normalizar(texto: String): String = texto.trim().lowercase()
    .replace('á', 'a')
    .replace('é', 'e')
    .replace('í', 'i')
    .replace('ó', 'o')
    .replace('ú', 'u')

fun colorHexDesde(nombre: String): Long {
    val normalizado = normalizar(nombre)
    if (normalizado.isBlank()) return 0xFF2C3E50
    return PALETA_COLORES.firstOrNull { normalizado.contains(it.first) }?.second ?: 0xFF2C3E50
}

fun coloresDesde(texto: String): List<Long> = texto
    .split(',', ';', '/', '|')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { colorHexDesde(it) }
    .distinct()
