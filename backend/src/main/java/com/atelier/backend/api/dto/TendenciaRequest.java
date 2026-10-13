package com.atelier.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.atelier.backend.dominio.FuenteTendencia;
import com.atelier.backend.dominio.Region;

/** Datos para registrar una tendencia capturada. */
public record TendenciaRequest(
        @NotBlank(message = "el titulo es obligatorio")
        @Size(max = 200, message = "el titulo no puede superar 200 caracteres")
        String titulo,
        @NotNull(message = "la region es obligatoria")
        Region region,
        @NotNull(message = "la fuente es obligatoria")
        FuenteTendencia fuente,
        @NotBlank(message = "la url de origen es obligatoria")
        @Size(max = 500, message = "la url no puede superar 500 caracteres")
        String urlOrigen,
        @Size(max = 500) String imagenUrl,
        @Size(max = 200) String colores,
        @Size(max = 40) String estilo,
        @Size(max = 80) String tela) {
}
