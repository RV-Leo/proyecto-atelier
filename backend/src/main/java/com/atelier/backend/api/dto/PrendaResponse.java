package com.atelier.backend.api.dto;

import java.time.Instant;

import com.atelier.backend.dominio.EstadoPrenda;
import com.atelier.backend.dominio.Prenda;

/** Respuesta con los datos de una prenda. */
public record PrendaResponse(
        Long id,
        String nombre,
        String slug,
        String tipoPrenda,
        String estilo,
        String colores,
        String tela,
        String descripcion,
        EstadoPrenda estado,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static PrendaResponse desde(Prenda p) {
        return new PrendaResponse(
                p.getId(),
                p.getNombre(),
                p.getSlug(),
                p.getTipoPrenda(),
                p.getEstilo(),
                p.getColores(),
                p.getTela(),
                p.getDescripcion(),
                p.getEstado(),
                p.getCreadoEn(),
                p.getActualizadoEn());
    }
}
