package com.atelier.backend.api.dto;

import java.util.List;

/**
 * Texto generado por el proveedor de IA.
 *
 * @param respuesta lo que se muestra en la conversación
 * @param imagenes  URLs de las propuestas generadas en este turno, si el
 *                  asistente usó la herramienta {@code generar_imagen}
 */
public record ChatIaResponse(String respuesta, List<String> imagenes) {

    public ChatIaResponse(String respuesta) {
        this(respuesta, List.of());
    }
}
