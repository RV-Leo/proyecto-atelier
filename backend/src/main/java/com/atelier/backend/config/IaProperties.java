package com.atelier.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del proveedor de IA y el almacenamiento de imágenes. */
@ConfigurationProperties(prefix = "atelier.ia")
public record IaProperties(
        String url,
        String apiKey,
        String modeloTexto,
        String modeloImagen,
        int timeoutSegundos,
        String directorioImagenes) {

    /** Indica si hay una URL de proveedor configurada. */
    public boolean habilitada() {
        return url != null && !url.isBlank();
    }
}
