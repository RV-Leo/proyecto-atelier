package com.atelier.backend.ia;

import java.util.List;

/**
 * Fuente de imágenes del diseñador.
 *
 * <p>Se separa de {@link ClienteIa} porque los generadores de imagen no tienen
 * nada que ver con el de texto: hablan protocolos distintos, tienen límites de
 * uso distintos y se pueden sustituir por separado. El texto puede seguir
 * viniendo de Gemini mientras las imágenes salen de otro proveedor.
 */
public interface GeneradorImagenes {

    /**
     * Genera imágenes a partir de una instrucción ya redactada.
     *
     * @param prompt instrucción para el modelo de imágenes
     * @param cantidad cuántas propuestas distintas pedir
     * @return una entrada por imagen generada; nunca vacía
     * @throws IaNoDisponibleException si el proveedor no devolvió ninguna
     *         imagen utilizable
     */
    List<ClienteIa.ImagenGenerada> generar(String prompt, int cantidad);
}
