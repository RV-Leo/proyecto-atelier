package com.atelier.backend.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.atelier.backend.dominio.EstadoMaterial;
import com.atelier.backend.dominio.Material;
import com.atelier.backend.dominio.UnidadMedida;

/**
 * Respuesta con los datos de un material del inventario.
 *
 * <p>{@code estado} se calcula aquí y no se guarda en base de datos: es la misma
 * clasificación (agotado / bajo / disponible) que la web ya mostraba, ahora
 * calculada en un solo sitio para que web y móvil vean lo mismo.
 */
public record MaterialResponse(
        Long id,
        String nombre,
        String tipo,
        String color,
        UnidadMedida unidad,
        BigDecimal stock,
        BigDecimal stockMinimo,
        EstadoMaterial estado,
        String proveedor,
        BigDecimal precioUnitario,
        boolean activo,
        Instant actualizadoEn) {

    public static MaterialResponse desde(Material m) {
        return new MaterialResponse(
                m.getId(),
                m.getNombre(),
                m.getTipo(),
                m.getColor(),
                m.getUnidad(),
                m.getStock(),
                m.getStockMinimo(),
                EstadoMaterial.de(m.getStock(), m.getStockMinimo()),
                m.getProveedor(),
                m.getPrecioUnitario(),
                m.isActivo(),
                m.getActualizadoEn());
    }
}
