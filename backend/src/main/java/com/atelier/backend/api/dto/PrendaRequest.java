package com.atelier.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una prenda (módulo de diseño generativo).
 *
 * <p>Coinciden con los campos que el usuario describe en la web: tipo, estilo,
 * colores y tela (README).
 */
public record PrendaRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede superar 120 caracteres")
        String nombre,
        @NotBlank(message = "el tipo de prenda es obligatorio")
        @Size(max = 40, message = "el tipo no puede superar 40 caracteres")
        String tipoPrenda,
        @NotBlank(message = "el estilo es obligatorio")
        @Size(max = 40, message = "el estilo no puede superar 40 caracteres")
        String estilo,
        @Size(max = 200, message = "los colores no pueden superar 200 caracteres")
        String colores,
        @Size(max = 80, message = "la tela no puede superar 80 caracteres")
        String tela,
        @Size(message = "la descripcion no puede ser tan larga")
        String descripcion) {
}
