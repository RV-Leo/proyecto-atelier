package com.atelier.backend.ia;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.atelier.backend.config.IaProperties;

/**
 * Configuración del puente de IA.
 *
 * <p>Elige el cliente según haya o no un servidor de modelos configurado:
 *
 * <ul>
 *   <li>Gemini gestiona las conversaciones mediante su API Interactions.</li>
 *   <li>Pollinations recibe por separado las solicitudes de generación y edición
 *       de imágenes, junto con su propia clave.</li>
 *   <li>Sin esas propiedades, usa {@link ClienteIaNoDisponible}: la API sigue
 *       funcionando y la viabilidad se calcula igual, solo que sin texto
 *       redactado por el modelo.</li>
 * </ul>
 *
 * <p>También publica las imágenes generadas, que el modelo devuelve en base64
 * y se guardan en disco.
 */
@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class ConfigIa implements WebMvcConfigurer {

    private final IaProperties propiedades;

    public ConfigIa(IaProperties propiedades) {
        this.propiedades = propiedades;
    }

    /**
     * Plantilla para construir clientes HTTP.
     *
     * <p>Spring Boot 4 ya no autoconfigura {@code RestClient.Builder} (no existe
     * el módulo {@code spring-boot-restclient} de versiones anteriores), así que
     * hay que declararlo. Es prototipo a propósito: cada cliente obtiene el suyo
     * y no comparten configuración por accidente.
     */
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Fábrica del puente de IA.
     *
     * <p>Cada proveedor se instancia solo si cuenta con URL y credenciales. Las
     * claves nunca se comparten entre Gemini y Pollinations.
     */
    @Bean
    ClienteIa clienteIa(GuardadorImagenes guardador, RestClient.Builder builder) {
        ClienteIa texto = propiedades.habilitada() && propiedades.apiKey() != null
                && !propiedades.apiKey().isBlank()
                        ? new ClienteGemini(propiedades, builder)
                        : new ClienteIaNoDisponible();
        if (propiedades.imagenExterna() && propiedades.apiKeyImagen() != null
                && !propiedades.apiKeyImagen().isBlank()) {
            GeneradorImagenes imagenes = new ClienteImagenPollinations(
                    propiedades, guardador, RestClient.builder());
            return new ClienteIaCompuesto(texto, imagenes);
        }
        return texto;
    }

    /**
     * Sirve bajo {@code /imagenes/} las imágenes que el modelo de diseño devolvió
     * en base64 y que se guardaron en {@code atelier.ia.directorio-imagenes}.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        // Path.toUri() ya devuelve "file:///...", por eso no se antepone "file:"
        // a mano: hacerlo dejaba el esquema duplicado ("file:file:///...").
        String ubicacion = Path.of(propiedades.directorioImagenes()).toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registro.addResourceHandler("/imagenes/**")
                .addResourceLocations(ubicacion);
    }
}
