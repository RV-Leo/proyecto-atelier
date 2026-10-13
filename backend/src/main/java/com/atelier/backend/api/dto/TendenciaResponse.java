package com.atelier.backend.api.dto;

import java.time.Instant;

import com.atelier.backend.dominio.FuenteTendencia;
import com.atelier.backend.dominio.Region;
import com.atelier.backend.dominio.Tendencia;

/** Respuesta con una tendencia capturada. */
public record TendenciaResponse(
        Long id,
        String titulo,
        Region region,
        FuenteTendencia fuente,
        String urlOrigen,
        String imagenUrl,
        String colores,
        String estilo,
        String tela,
        Instant capturadoEn) {

    public static TendenciaResponse desde(Tendencia t) {
        return new TendenciaResponse(
                t.getId(),
                t.getTitulo(),
                t.getRegion(),
                t.getFuente(),
                t.getUrlOrigen(),
                t.getImagenUrl(),
                t.getColores(),
                t.getEstilo(),
                t.getTela(),
                t.getCapturadoEn());
    }
}
