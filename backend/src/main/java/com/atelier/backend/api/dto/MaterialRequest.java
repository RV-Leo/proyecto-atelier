package com.atelier.backend.api.dto;

import java.math.BigDecimal;

import com.atelier.backend.dominio.UnidadMedida;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un material del inventario.
 *
 * @param stock cantidad disponible actual
 * @param stockMinimo umbral por debajo del cual hay que reponer
 * @param activo si es {@code null} se asume {@code true}
 */
public record MaterialRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no puede superar 150 caracteres")
        String nombre,
        @NotBlank(message = "el tipo es obligatorio")
        @Size(max = 40, message = "el tipo no puede superar 40 caracteres")
        String tipo,
        @Size(max = 50, message = "el color no puede superar 50 caracteres")
        String color,
        @NotNull(message = "la unidad es obligatoria")
        UnidadMedida unidad,
        @NotNull(message = "el stock es obligatorio")
        @DecimalMin(value = "0", message = "el stock no puede ser negativo")
        BigDecimal stock,
        @NotNull(message = "el stock minimo es obligatorio")
        @DecimalMin(value = "0", message = "el stock minimo no puede ser negativo")
        BigDecimal stockMinimo,
        @Size(max = 120, message = "el proveedor no puede superar 120 caracteres")
        String proveedor,
        @DecimalMin(value = "0", message = "el precio no puede ser negativo")
        BigDecimal precioUnitario,
        Boolean activo) {
}
