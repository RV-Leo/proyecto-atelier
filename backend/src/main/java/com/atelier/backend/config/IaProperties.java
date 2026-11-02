package com.atelier.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del puente de IA.
 *
 * <p>El README ubica al backend entre la aplicación y los modelos Qwen. Estas
 * propiedades apuntan al servidor de inferencia (o al gateway que lo rodea).
 *
 * <p>Si {@code url} está vacía, el backend funciona sin IA: los cálculos de
 * stock y viabilidad se siguen haciendo igual, solo cambia que el comentario
 * se redacta con una plantilla en lugar de con el modelo. Así el proyecto no
 * se cae mientras el servidor de modelos no esté listo.
 *
 * @param url base del servidor compatible con OpenAI, p. ej.
 *        {@code http://localhost:11434/v1}. Vacía desactiva la IA.
 * @param apiKey clave si el servidor la exige; vacía si no.
 * @param modeloTexto modelo de lenguaje (README: Qwen3.6-35B-A3B).
 * @param modeloImagen modelo de imágenes (README: Qwen-Image).
 * @param timeoutSegundos tiempo máximo de espera antes de rendirse.
 * @param directorioImagenes dónde se guardan las imágenes generadas en base64.
 */
@ConfigurationProperties(prefix = "atelier.ia")
public record IaProperties(
        String url,
        String apiKey,
        String modeloTexto,
        String modeloImagen,
        int timeoutSegundos,
        String directorioImagenes) {

    /** La IA solo se usa si hay un servidor al que preguntarle. */
    public boolean habilitada() {
        return url != null && !url.isBlank();
    }
}
