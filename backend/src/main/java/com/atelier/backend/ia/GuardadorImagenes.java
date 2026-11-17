package com.atelier.backend.ia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.atelier.backend.config.IaProperties;

/** Guarda imágenes generadas que el proveedor devuelve en base64. */
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

            return "/imagenes/" + directorio.relativize(destino).toString().replace('\\', '/');
        } catch (IllegalArgumentException | IOException e) {
            throw new IaNoDisponibleException("No se pudo guardar la imagen generada", e);
        }
    }
}
