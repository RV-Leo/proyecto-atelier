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

            byte[] bytes = Base64.getDecoder().decode(base64);
            String extension = extension(bytes);
            Path destino = carpeta.resolve(UUID.randomUUID() + extension);
            Files.write(destino, bytes);

            // Ruta relativa: el gestor de estáticos la sirve bajo /imagenes/.
            return "/imagenes/" + directorio.relativize(destino).toString().replace('\\', '/');
        } catch (IllegalArgumentException | IOException e) {
            throw new IaNoDisponibleException("No se pudo guardar la imagen generada", e);
        }
    }

    private String extension(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47) {
            return ".png";
        }
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8
                && bytes[2] == (byte) 0xff) {
            return ".jpg";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P') {
            return ".webp";
        }
        throw new IaNoDisponibleException("El modelo devolvió un formato de imagen no compatible");
    }
}
