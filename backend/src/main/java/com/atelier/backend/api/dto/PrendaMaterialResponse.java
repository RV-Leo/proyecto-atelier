package com.atelier.backend.api.dto;

import java.math.BigDecimal;

import com.atelier.backend.dominio.UnidadMedida;

/** Un material de la ficha técnica de una prenda. */
public record PrendaMaterialResponse(
        Long id,
        Long materialId,
        String material,
        UnidadMedida unidad,
        BigDecimal cantidadRequerida,
        BigDecimal stockDisponible,
        boolean suficiente) {
}
