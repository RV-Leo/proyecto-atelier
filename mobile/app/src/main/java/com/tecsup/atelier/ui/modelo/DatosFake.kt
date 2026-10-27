package com.tecsup.atelier.ui.modelo

object DatosFake {
    val categorias = listOf(
        Categoria("all", "Todos"),
        Categoria("polos", "Polos"),
        Categoria("pantalones", "Pantalones"),
        Categoria("casacas", "Casacas & Sacos"),
        Categoria("camisas", "Camisas"),
        Categoria("poleras", "Poleras"),
        Categoria("vestidos", "Vestidos")
    )

    val productos = listOf(
        Producto(
            id = "1",
            nombre = "Polo Pima Premium Minimalist",
            idCategoria = "polos",
            nombreCategoria = "Polos",
            precio = 69.90,
            precioOriginal = 89.90,
            calificacion = 4.8,
            cantidadResenas = 124,
            descripcion = "Polo de algodón 100% Pima peruano de alta suavidad y ajuste perfecto.",
            esPopular = true,
            esNuevo = true,
            esFavorito = true,
            colorHex = 0xFF2C3E50
        ),
        Producto(
            id = "2",
            nombre = "Pantalón Jean Slim Fit Azul",
            idCategoria = "pantalones",
            nombreCategoria = "Pantalones",
            precio = 129.90,
            precioOriginal = 159.90,
            calificacion = 4.6,
            cantidadResenas = 89,
            descripcion = "Jean clásico elaborado en denim elástico para máxima comodidad diario.",
            esPopular = true,
            esFavorito = false,
            colorHex = 0xFF1B3B6F
        ),
        Producto(
            id = "3",
            nombre = "Casaca Denim Vintage Oversize",
            idCategoria = "casacas",
            nombreCategoria = "Casacas",
            precio = 189.90,
            precioOriginal = 229.90,
            calificacion = 4.9,
            cantidadResenas = 56,
            descripcion = "Casaca de jeans estilo retro con acabado lavado y botones metálicos.",
            esPopular = true,
            esNuevo = true,
            esFavorito = true,
            colorHex = 0xFF4B6584
        ),
        Producto(
            id = "4",
            nombre = "Camisa Lino Manga Larga Blanca",
            idCategoria = "camisas",
            nombreCategoria = "Camisas",
            precio = 119.90,
            calificacion = 4.7,
            cantidadResenas = 42,
            descripcion = "Camisa fresca de lino natural ideal para climas cálidos y eventos casuales.",
            esFavorito = false,
            colorHex = 0xFFD3D3D3
        ),
        Producto(
            id = "5",
            nombre = "Polera Hooded Cotton Urban",
            idCategoria = "poleras",
            nombreCategoria = "Poleras",
            precio = 99.90,
            precioOriginal = 129.90,
            calificacion = 4.5,
            cantidadResenas = 78,
            descripcion = "Polera con capucha de franja felpada de textura suave e interior abrigador.",
            esPopular = true,
            esFavorito = true,
            colorHex = 0xFF37474F
        ),
        Producto(
            id = "6",
            nombre = "Pantalón Chino Casual Beige",
            idCategoria = "pantalones",
            nombreCategoria = "Pantalones",
            precio = 114.90,
            calificacion = 4.6,
            cantidadResenas = 63,
            descripcion = "Pantalón versátil de dril algodón suave, perfecto para oficina y fin de semana.",
            esFavorito = false,
            colorHex = 0xFFD7CCC8
        ),
        Producto(
            id = "7",
            nombre = "Polo Graphic Art Streetwear",
            idCategoria = "polos",
            nombreCategoria = "Polos",
            precio = 54.90,
            precioOriginal = 69.90,
            calificacion = 4.4,
            cantidadResenas = 95,
            descripcion = "Polo cuello redondo con estampado gráfico exclusivo en la espalda.",
            esFavorito = false,
            colorHex = 0xFF78909C
        ),
        Producto(
            id = "8",
            nombre = "Vestido Casual Verano Floral",
            idCategoria = "vestidos",
            nombreCategoria = "Vestidos",
            precio = 149.90,
            calificacion = 4.8,
            cantidadResenas = 31,
            descripcion = "Vestido ligero con estampado de primavera y silueta fluida.",
            esNuevo = true,
            esFavorito = true,
            colorHex = 0xFF8E24AA
        )
    )
}
