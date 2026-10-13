package com.atelier.backend.dominio;

import java.math.BigDecimal;

/**
 * Estado del stock de un material, calculado y nunca guardado en base de datos.
 *
 * <p>La web ya muestra estos tres estados en su inventario
 * ({@code catalogo/inventario.html}) y los calcula en Python. Como ahora consume
 * esta API, el cálculo vive aquí para que las dos vistas (y la app móvil) vean
 * exactamente lo mismo.
 */
public enum EstadoMaterial {

    /** No queda nada: hay que reponer antes de poder producir. */
    AGOTADO,

    /** Hay, pero por debajo del mínimo acordado. */
    BAJO,

    /** Hay stock por encima del mínimo. */
    DISPONIBLE;

    /**
     * Mismos umbrales que los que ya usa la web: agotado si {@code stock <= 0},
     * bajo si {@code 0 < stock <= stockMinimo}, disponible en el resto.
     */
    public static EstadoMaterial de(BigDecimal stock, BigDecimal stockMinimo) {
        if (stock == null || stock.compareTo(BigDecimal.ZERO) <= 0) {
            return AGOTADO;
        }
        if (stockMinimo != null && stock.compareTo(stockMinimo) <= 0) {
            return BAJO;
        }
        return DISPONIBLE;
    }
}
