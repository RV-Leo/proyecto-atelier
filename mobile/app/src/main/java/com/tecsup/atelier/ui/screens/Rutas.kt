package com.tecsup.atelier.ui.screens

import com.tecsup.atelier.R

sealed class Rutas(
    val ruta: String,
    val titulo: String,
    val icono: Int
) {
    data object Inicio : Rutas("inicio", "Home", R.drawable.ic_home)
    data object Pedido : Rutas("pedido", "Pedido", R.drawable.ic_orders)
    data object IA : Rutas("ia", "IA", R.drawable.ic_ai_assistant)
    data object Perfil : Rutas("perfil", "Perfil", R.drawable.ic_account_box)

    companion object {
        fun items(): List<Rutas> = listOf(Inicio, Pedido, IA, Perfil)
    }
}
