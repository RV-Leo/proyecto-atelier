package com.atelier.backend.ia;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Una herramienta que el modelo de texto puede pedir ejecutar.
 *
 * <p>El modelo no ejecuta nada por sí solo: describe qué herramienta quiere y
 * con qué argumentos, y el orquestador ({@code AsistenteAtelier}) decide si la
 * ejecuta. Así el modelo no puede inventar stock ni imágenes.
 *
 * @param nombre      identificador que usa el modelo, p. ej. {@code generar_imagen}
 * @param descripcion qué hace y cuándo usarla; es lo que el modelo lee
 * @param parametros  esquema JSON de los argumentos (tipo {@code object})
 */
public record Herramienta(String nombre, String descripcion, Map<String, Object> parametros) {

    /**
     * Declaración en el formato que espera la API Interactions de Gemini.
     *
     * <p>Es un mapa y no la herramienta en sí porque el protocolo HTTP pide
     * {@code type: "function"} junto al nombre, la descripción y el esquema.
     */
    public Map<String, Object> declaracion() {
        Map<String, Object> declaracion = new LinkedHashMap<>();
        declaracion.put("type", "function");
        declaracion.put("name", nombre);
        declaracion.put("description", descripcion);
        declaracion.put("parameters", parametros);
        return declaracion;
    }
}
