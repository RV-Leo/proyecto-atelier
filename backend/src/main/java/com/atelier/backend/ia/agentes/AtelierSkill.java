package com.atelier.backend.ia.agentes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Carga el skill de dominio que acompaña cada solicitud enviada a Gemini. */
@Component
public class AtelierSkill {

    private final String instrucciones;

    public AtelierSkill() {
        try (InputStream contenido = new ClassPathResource("skills/atelier-project.md").getInputStream()) {
            instrucciones = new String(contenido.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo cargar el skill de ATELIER.", error);
        }
    }

    public String instrucciones() {
        return instrucciones;
    }
}
