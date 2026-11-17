package com.atelier.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Instrucción y referencia visual para editar un diseño. */
public record EditarImagenIaRequest(
        @NotBlank @Size(max = 2000) String prompt,
        @NotBlank @Size(max = 12_000_000) String imageBase64,
        @NotBlank @Pattern(regexp = "image/(png|jpeg|webp)") String contentType) {
}
