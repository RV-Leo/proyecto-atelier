package com.atelier.backend.dominio;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Convierte un texto en un slug apto para URL.
 *
 * <p>Reproduce el comportamiento de {@code django.utils.text.slugify} porque la
 * web construye sus enlaces con ese algoritmo: si los slugs no coinciden, los
 * enlaces del catálogo dejarían de funcionar.
 */
public final class Slug {

    private Slug() {
    }

    /** {@code "Vestido de verano"} → {@code "vestido-de-verano"}. */
    public static String de(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
