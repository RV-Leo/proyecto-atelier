package com.atelier.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del puente de IA.
 *
 * <p>Los proveedores de texto e imagen tienen URL y claves independientes.
 *
 * <p>Si {@code url} está vacía, el backend funciona sin IA: los cálculos de
 * stock y viabilidad se siguen haciendo igual, solo cambia que el comentario
 * se redacta con una plantilla en lugar de con el modelo. Así el proyecto no
 * se cae mientras el servidor de modelos no esté listo.
 *
 * <p>{@code urlImagen} es opcional y permite que el generador de imágenes sea
 * distinto del de texto: no todos los proveedores hablan el mismo protocolo.
 * Pollinations, por ejemplo, no expone {@code /images/generations} sino un
 * {@code GET /prompt/...} que devuelve los bytes de la foto, así que necesita su
 * propio cliente. Vacía, las imágenes siguen saliendo del servidor de {@code url}.
 *
 * @param url base de Gemini, normalmente {@code https://generativelanguage.googleapis.com/v1beta}.
 * @param apiKey clave privada de Gemini.
 * @param modeloTexto modelo de lenguaje configurado en el proveedor.
 * @param modeloImagen modelo de imágenes configurado en Pollinations.
 * @param timeoutSegundos tiempo máximo de espera antes de rendirse.
 * @param directorioImagenes dónde se guardan las imágenes generadas en base64.
 * @param urlImagen base de la API de imágenes de Pollinations.
 * @param apiKeyImagen clave privada de Pollinations, separada de la de Gemini.
 */
@ConfigurationProperties(prefix = "atelier.ia")
public record IaProperties(
        String url,
        String apiKey,
        String modeloTexto,
        String modeloImagen,
        int timeoutSegundos,
        String directorioImagenes,
        String urlImagen,
        String apiKeyImagen) {

    /** Compatibilidad con las configuraciones anteriores del backend. */
    public IaProperties(
            String url,
            String apiKey,
            String modeloTexto,
            String modeloImagen,
            int timeoutSegundos,
            String directorioImagenes,
            String urlImagen) {
        this(url, apiKey, modeloTexto, modeloImagen, timeoutSegundos, directorioImagenes, urlImagen, "");
    }

    /** El cliente de texto solo se usa si Gemini está configurado. */
    public boolean habilitada() {
        return url != null && !url.isBlank();
    }

    /** Si las imágenes se piden a un proveedor distinto del de texto. */
    public boolean imagenExterna() {
        return urlImagen != null && !urlImagen.isBlank();
    }
}
