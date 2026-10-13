package com.atelier.backend.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.atelier.backend.dominio.Material;
import com.atelier.backend.dominio.UnidadMedida;

/** Respuesta con los datos de un material del inventario. */
public record MaterialResponse(
        Long id,
        String nombre,
        String tipo,
        String color,
        UnidadMedida unidad,
        BigDecimal stock,
        BigDecimal stockMinimo,
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
                m.getProveedor(),
                m.getPrecioUnitario(),
                m.isActivo(),
                m.getActualizadoEn());
    }
}
