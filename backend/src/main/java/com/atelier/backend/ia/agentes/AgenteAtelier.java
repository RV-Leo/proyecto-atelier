package com.atelier.backend.ia.agentes;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Especialista de chat que enruta por intención y añade instrucciones acotadas. */
public record AgenteAtelier(String nombre, String instrucciones, List<String> temas) {

    public boolean reconoce(String texto) {
        String normalizado = normalizar(texto);
        return temas.stream().map(AgenteAtelier::normalizar)
                .anyMatch(tema -> (" " + normalizado + " ").contains(" " + tema + " "));
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
