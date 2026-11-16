package com.atelier.backend.api.dto;

import java.time.Instant;

import com.atelier.backend.dominio.PropuestaDiseno;

/**
 * Una propuesta de imagen generada por IA para una prenda.
 *
 * @param imagenUrl dónde está la imagen; puede ser una URL remota o una ruta
 *                  local bajo {@code /imagenes/} cuando el modelo la devolvió
 *                  en base64
 */
public record PropuestaDisenoResponse(
        Long id,
        Long prendaId,
        String imagenUrl,
        String promptUsado,
        boolean seleccionada,
        Instant creadoEn) {

    public static PropuestaDisenoResponse desde(PropuestaDiseno p) {
        return new PropuestaDisenoResponse(
                p.getId(),
                p.getPrenda().getId(),
                p.getImagenUrl(),
                p.getPromptUsado(),
                p.isSeleccionada(),
                p.getCreadoEn());
    }
}
