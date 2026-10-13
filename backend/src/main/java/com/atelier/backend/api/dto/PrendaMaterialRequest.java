package com.atelier.backend.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Asigna un material a la ficha técnica de una prenda.
 *
 * @param cantidadRequerida cuánto material necesita la prenda para producirse
 */
public record PrendaMaterialRequest(
        @NotNull(message = "el id del material es obligatorio")
        Long materialId,
        @NotNull(message = "la cantidad requerida es obligatoria")
        @DecimalMin(value = "0", message = "la cantidad no puede ser negativa")
        BigDecimal cantidadRequerida) {
}
