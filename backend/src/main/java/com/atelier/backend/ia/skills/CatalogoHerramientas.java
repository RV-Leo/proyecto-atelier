package com.atelier.backend.ia.skills;

import java.util.List;
import java.util.Map;

import com.atelier.backend.ia.Herramienta;

/**
 * Herramientas que el asistente puede pedir al modelo.
 *
 * <p>El catálogo es explícito y corto a propósito: cada herramienta nueva es
 * una capacidad real del backend, no una sugerencia al modelo. Si no está aquí,
 * el asistente no puede ejecutarla.
 */
public final class CatalogoHerramientas {

    public static final String GENERAR_IMAGEN = "generar_imagen";
    public static final String CONSULTAR_STOCK = "consultar_stock";

    private CatalogoHerramientas() {
    }

    /** Todas las herramientas disponibles para una conversación. */
    public static List<Herramienta> todas() {
        return List.of(generarImagen(), consultarStock());
    }

    private static Herramienta generarImagen() {
        return new Herramienta(
                GENERAR_IMAGEN,
                "Genera una propuesta visual de una prenda ya existente. Úsala solo cuando el "
                        + "usuario pida ver, crear, variar o retocar un diseño. La instrucción debe "
                        + "describir la prenda completa (tipo, corte, colores, tela, detalles, fondo "
                        + "neutro e iluminación de estudio), sin marcas ni logos. El proveedor de "
                        + "imágenes crea una imagen nueva: no edita píxeles.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "prenda_id", Map.of(
                                        "type", "integer",
                                        "description", "Id de la prenda existente sobre la que se genera la propuesta."),
                                "instruccion", Map.of(
                                        "type", "string",
                                        "description", "Brief visual completo en español para el generador de imágenes.")),
                        "required", List.of("prenda_id", "instruccion")));
    }

    private static Herramienta consultarStock() {
        return new Herramienta(
                CONSULTAR_STOCK,
                "Consulta la ficha técnica de una prenda y el stock real de cada material. "
                        + "Úsala cuando el usuario pregunte si puede producir la prenda, qué materiales "
                        + "necesita o qué le falta. Devuelve cantidades, stock y si alcanza.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "prenda_id", Map.of(
                                        "type", "integer",
                                        "description", "Id de la prenda cuya ficha técnica se consulta.")),
                        "required", List.of("prenda_id")));
    }
}
