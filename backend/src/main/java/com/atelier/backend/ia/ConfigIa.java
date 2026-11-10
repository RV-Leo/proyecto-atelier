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
 *   <li>Con {@code atelier.ia.url}, usa {@link ClienteIaHttp}, que habla con un
 *       servidor compatible con OpenAI (vLLM, Ollama, LM Studio...).</li>
 *   <li>Con {@code atelier.ia.url-imagen} además, las imágenes se piden a ese
 *       proveedor a través de {@link ClienteImagenPollinations}, que usa un
 *       protocolo propio. Así el texto puede venir de un modelo y las fotos de
 *       otro, que es lo que permite usar la vía gratuita de Pollinations sin
 *       dejar de aprovechar el servidor de texto ya configurado.</li>
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
     * <p>La fórmula es: texto de donde haya, imágenes de donde haya, y si no
     * hay nada, la vía determinista. Con {@code url-imagen} definida se compone
     * un cliente por fuente, de modo que el generador de fotos gratuito no
     * recibe la cabecera Authorization del proveedor de texto.
     */
    @Bean
    ClienteIa clienteIa(GuardadorImagenes guardador, RestClient.Builder builder) {
        if (!propiedades.habilitada()) {
            return new ClienteIaNoDisponible();
        }

        ClienteIa texto = new ClienteIaHttp(propiedades, guardador, builder);
        if (!propiedades.imagenExterna()) {
            // Sin proveedor de imágenes propio, el mismo cliente hace las dos
            // cosas y se comporta exactamente como hasta ahora.
            return texto;
        }

        GeneradorImagenes imagenes =
                new ClienteImagenPollinations(propiedades, guardador, RestClient.builder());
        return new ClienteIaCompuesto(texto, imagenes);
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
