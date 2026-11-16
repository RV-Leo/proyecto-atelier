package com.atelier.backend.ia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.atelier.backend.config.IaProperties;

/**
 * Guarda en disco las imágenes que el modelo devuelve en base64.
 *
 * <p>No todos los servidores devuelven una URL; muchos devuelven los bytes
 * codificados. La columna {@code imagen_url} espera una referencia accesible,
 * así que la imagen se escribe en un directorio local y se devuelve su ruta.
 */
@Component
public class GuardadorImagenes {

    private final Path directorio;

    public GuardadorImagenes(IaProperties propiedades) {
        this.directorio = Path.of(propiedades.directorioImagenes());
    }

    public String guardar(String base64) {
        try {
            Path carpeta = directorio.resolve(LocalDate.now().toString());
            Files.createDirectories(carpeta);

            Path destino = carpeta.resolve(UUID.randomUUID() + ".png");
            Files.write(destino, Base64.getDecoder().decode(base64));

            // Ruta relativa: el gestor de estáticos la sirve bajo /imagenes/.
            return "/imagenes/" + directorio.relativize(destino).toString().replace('\\', '/');
        } catch (IllegalArgumentException | IOException e) {
            throw new IaNoDisponibleException("No se pudo guardar la imagen generada", e);
        }
    }
}
