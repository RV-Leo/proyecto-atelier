package com.atelier.backend.api.dto;

/**
 * El prompt que se usaría para generar imágenes de una prenda.
 *
 * <p>Se expone aparte de la generación para que quien llama pueda revisarlo o
 * corregirlo antes de gastar tiempo de GPU.
 *
 * @param fuente quién redactó el prompt: {@code MODELO} si lo hizo la IA,
 *               {@code PLANTILLA} si se armó con una regla fija porque no hay
 *               servidor de modelos disponible
 */
public record PromptResponse(String prompt, String fuente) {

    public static final String FUENTE_MODELO = "MODELO";
    public static final String FUENTE_PLANTILLA = "PLANTILLA";
}
