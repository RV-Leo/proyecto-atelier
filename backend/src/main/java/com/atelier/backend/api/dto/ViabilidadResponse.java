package com.atelier.backend.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.atelier.backend.dominio.UnidadMedida;

/**
 * Resultado del análisis de viabilidad de una prenda contra el stock real.
 *
 * @param viable si todos los materiales necesarios están disponibles
 * @param puntaje 0 a 100: porcentaje de materiales con stock suficiente
 * @param comentario explicación en lenguaje natural (la redacta la IA)
 * @param detalles desglose por material
 * @param alternativas sugerencias para los materiales que faltan
 */
public record ViabilidadResponse(
        Long analisisId,
        Long prendaId,
        String prenda,
        boolean viable,
        int puntaje,
        String comentario,
        List<Detalle> detalles,
        List<Alternativa> alternativas) {

    /** Estado de un material puntual dentro del análisis. */
    public record Detalle(
            Long materialId,
            String material,
            UnidadMedida unidad,
            BigDecimal requerido,
            BigDecimal disponible,
            boolean suficiente) {
    }

    /** Alternativa propuesta cuando un material no alcanza. */
    public record Alternativa(
            Long materialId,
            String material,
            UnidadMedida unidad,
            BigDecimal stockDisponible,
            String motivo) {
    }
}
