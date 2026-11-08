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
 * Guarda en disco las imágenes que devuelve el modelo.
 *
 * <p>No todos los proveedores devuelven lo mismo: algunos mandan los bytes en
 * base64 dentro de un JSON, y otros como Pollinations devuelven directamente
 * el archivo (JPEG). Ambos casos acaban aquí, y la extensión se elige mirando
 * las firmas mágicas del archivo para que el gestor de estáticos sirva el
 * Content-Type correcto.
 */
@Component
public class GuardadorImagenes {

    private final Path directorio;

    public GuardadorImagenes(IaProperties propiedades) {
        this.directorio = Path.of(propiedades.directorioImagenes());
    }

    public String guardar(String base64) {
        try {
            return guardar(Base64.getDecoder().decode(base64));
        } catch (IllegalArgumentException e) {
            throw new IaNoDisponibleException("No se pudo guardar la imagen generada", e);
        }
    }

    /**
     * Guarda los bytes tal cual, eligiendo la extensión por el formato real.
     *
     * <p>Sin esto, un JPEG servido bajo un nombre {@code .png} llegaría con un
     * Content-Type equivocado.
     */
    public String guardar(byte[] bytes) {
        try {
            Path carpeta = directorio.resolve(LocalDate.now().toString());
            Files.createDirectories(carpeta);

            Path destino = carpeta.resolve(UUID.randomUUID() + "." + extensionDe(bytes));
            Files.write(destino, bytes);

            // Ruta relativa: el gestor de estáticos la sirve bajo /imagenes/.
            return "/imagenes/" + directorio.relativize(destino).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new IaNoDisponibleException("No se pudo guardar la imagen generada", e);
        }
    }

    /** Extensión según las firmas mágicas; png por defecto, como siempre. */
    private String extensionDe(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 4
                && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "png";
        }
        if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "gif";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return "png";
    }
}
