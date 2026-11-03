package com.tecsup.atelier.ui.modelo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object CarritoManager {
    private val _items = MutableStateFlow<List<ItemCarrito>>(emptyList())
    val items: StateFlow<List<ItemCarrito>> = _items.asStateFlow()

    fun agregarProducto(producto: Producto, talla: String = "M", cantidad: Int = 1) {
        _items.update { lista ->
            val existente = lista.find { it.producto.id == producto.id && it.talla == talla }
            if (existente != null) {
                lista.map {
                    if (it.producto.id == producto.id && it.talla == talla) {
                        it.copy(cantidad = it.cantidad + cantidad)
                    } else it
                }
            } else {
                lista + ItemCarrito(producto, talla, cantidad)
            }
        }
    }

    fun removerItem(item: ItemCarrito) {
        _items.update { lista ->
            lista.filterNot { it.producto.id == item.producto.id && it.talla == item.talla }
        }
    }

    fun actualizarCantidad(item: ItemCarrito, nuevaCantidad: Int) {
        if (nuevaCantidad <= 0) {
            removerItem(item)
            return
        }
        _items.update { lista ->
            lista.map {
                if (it.producto.id == item.producto.id && it.talla == item.talla) {
                    it.copy(cantidad = nuevaCantidad)
                } else it
            }
        }
    }

    fun limpiarCarrito() {
        _items.update { emptyList() }
    }

    fun obtenerSubtotal(): Double {
        return _items.value.sumOf { it.producto.precio * it.cantidad }
    }

    fun obtenerEnvio(): Double {
        return if (obtenerSubtotal() > 150.0 || _items.value.isEmpty()) 0.0 else 15.0
    }

    fun obtenerTotal(): Double {
        return obtenerSubtotal() + obtenerEnvio()
    }
}
