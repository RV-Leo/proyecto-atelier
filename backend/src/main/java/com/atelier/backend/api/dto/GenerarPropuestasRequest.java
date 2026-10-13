package com.atelier.backend.api.dto;

/**
 * Petición para generar propuestas de imagen de una prenda.
 *
 * @param cantidad cuántas propuestas distintas pedir; si se omite, el servidor
 *                 decide un valor razonable
 */
public record GenerarPropuestasRequest(Integer cantidad) {

    /** Cuántas imágenes pedir si quien llama no lo indica. */
    public static final int CANTIDAD_DEFECTO = 2;

    public int cantidadEfectiva() {
        if (cantidad == null) {
            return CANTIDAD_DEFECTO;
        }
        return Math.max(1, Math.min(cantidad, 4));
    }
}
