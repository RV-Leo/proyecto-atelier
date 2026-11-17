package com.atelier.backend.ia;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.atelier.backend.config.IaProperties;

/** Configura el cliente de IA y publica las imágenes generadas. */
@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class ConfigIa implements WebMvcConfigurer {

    private final IaProperties propiedades;

    public ConfigIa(IaProperties propiedades) {
        this.propiedades = propiedades;
    }

    /** Crea el builder HTTP usado por el cliente de IA. */
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    ClienteIa clienteIa(GuardadorImagenes guardador, RestClient.Builder builder) {
        return propiedades.habilitada()
                ? new ClienteIaHttp(propiedades, guardador, builder)
                : new ClienteIaNoDisponible();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String ubicacion = Path.of(propiedades.directorioImagenes()).toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registro.addResourceHandler("/imagenes/**")
                .addResourceLocations(ubicacion);
    }
}
