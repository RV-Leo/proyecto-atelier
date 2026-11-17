package com.atelier.backend.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.atelier.backend.api.dto.PrendaMaterialResponse;
import com.atelier.backend.api.dto.PropuestaDisenoResponse;
import com.atelier.backend.ia.ClienteIa;
import com.atelier.backend.ia.ClienteIa.EjecutorHerramientas;
import com.atelier.backend.ia.ClienteIa.MensajeChat;
import com.atelier.backend.ia.IaNoDisponibleException;
import com.atelier.backend.ia.skills.CatalogoHerramientas;

import jakarta.persistence.EntityNotFoundException;

/**
 * Asistente conversacional de Atelier.
 *
 * <p>Une las tres piezas de la skill: el prompt de negocio
 * ({@link com.atelier.backend.ia.skills.SkillAtelier}), el catálogo de
 * herramientas ({@link CatalogoHerramientas}) y la ejecución real contra los
 * servicios del backend. El modelo solo pide; aquí se decide qué se ejecuta y
 * con qué datos.
 *
 * <p>Ninguna herramienta inventa información: el stock sale de la ficha
 * técnica y las imágenes del generador configurado. Si una herramienta falla,
 * el error vuelve al modelo como resultado para que lo explique, en lugar de
 * romper la conversación.
 */
@Service
public class AsistenteAtelier {

    private static final Logger log = LoggerFactory.getLogger(AsistenteAtelier.class);

    /**
     * Pollinations gratuito permite una imagen cada 45 s por IP, así que el
     * asistente pide una sola propuesta por turno.
     */
    private static final int PROPUESTAS_POR_TURNO = 1;

    private final ClienteIa clienteIa;
    private final DisenoServicio diseno;
    private final ViabilidadServicio viabilidad;

    public AsistenteAtelier(ClienteIa clienteIa, DisenoServicio diseno, ViabilidadServicio viabilidad) {
        this.clienteIa = clienteIa;
        this.diseno = diseno;
        this.viabilidad = viabilidad;
    }

    /**
     * Responde al último mensaje, ejecutando las herramientas que el modelo pida.
     *
     * @return el texto para el usuario y las imágenes generadas en el turno
     */
    public Respuesta conversar(List<MensajeChat> mensajes, String contexto) {
        List<String> imagenes = new ArrayList<>();
        EjecutorHerramientas ejecutor = (nombre, argumentos) -> ejecutar(nombre, argumentos, imagenes);

        String texto = clienteIa.conversar(
                mensajes, contexto, CatalogoHerramientas.todas(), ejecutor);

        return new Respuesta(texto, List.copyOf(imagenes));
    }

    private Map<String, Object> ejecutar(String nombre, Map<String, Object> argumentos,
            List<String> imagenes) {
        return switch (nombre) {
            case CatalogoHerramientas.GENERAR_IMAGEN -> generarImagen(argumentos, imagenes);
            case CatalogoHerramientas.CONSULTAR_STOCK -> consultarStock(argumentos);
            default -> Map.of("error", "Herramienta desconocida: " + nombre);
        };
    }

    /** Genera una propuesta con el brief que redactó el modelo. */
    private Map<String, Object> generarImagen(Map<String, Object> argumentos, List<String> imagenes) {
        Long prendaId = numero(argumentos.get("prenda_id"));
        if (prendaId == null) {
            return Map.of("error", "Falta prenda_id: pide al usuario que elija una prenda.");
        }
        String instruccion = cadena(argumentos.get("instruccion"));
        if (instruccion == null) {
            return Map.of("error", "Falta la instrucción visual para generar la imagen.");
        }

        try {
            List<PropuestaDisenoResponse> propuestas =
                    diseno.generarPropuestas(prendaId, instruccion, PROPUESTAS_POR_TURNO);
            List<Map<String, Object>> generadas = new ArrayList<>();
            for (PropuestaDisenoResponse propuesta : propuestas) {
                imagenes.add(propuesta.imagenUrl());
                generadas.add(Map.of(
                        "id", propuesta.id(),
                        "imagenUrl", propuesta.imagenUrl()));
            }
            log.info("Asistente generó {} propuesta(s) para la prenda {}", generadas.size(), prendaId);
            return Map.of(
                    "prendaId", prendaId,
                    "propuestas", generadas,
                    "nota", "Es una imagen nueva, no una edición de píxeles de una foto anterior.");
        } catch (EntityNotFoundException error) {
            return Map.of("error", "No existe la prenda " + prendaId + ". Créala antes de pedir una imagen.");
        } catch (IaNoDisponibleException error) {
            log.warn("No se pudo generar la imagen de la prenda {}: {}", prendaId, error.getMessage());
            return Map.of("error",
                    "No se pudo generar la imagen: el generador no está disponible en este momento.");
        }
    }

    /** Devuelve la ficha técnica con el stock real de cada material. */
    private Map<String, Object> consultarStock(Map<String, Object> argumentos) {
        Long prendaId = numero(argumentos.get("prenda_id"));
        if (prendaId == null) {
            return Map.of("error", "Falta prenda_id: pide al usuario que elija una prenda.");
        }

        try {
            List<PrendaMaterialResponse> ficha = viabilidad.fichaTecnica(prendaId);
            List<Map<String, Object>> materiales = new ArrayList<>();
            for (PrendaMaterialResponse material : ficha) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("material", material.material());
                fila.put("unidad", material.unidad());
                fila.put("requerido", material.cantidadRequerida());
                fila.put("disponible", material.stockDisponible());
                fila.put("suficiente", material.suficiente());
                materiales.add(fila);
            }
            boolean puedeProducirse = !ficha.isEmpty()
                    && ficha.stream().allMatch(PrendaMaterialResponse::suficiente);
            return Map.of(
                    "prendaId", prendaId,
                    "puede_producirse", puedeProducirse,
                    "materiales", materiales,
                    "nota", ficha.isEmpty()
                            ? "La prenda no tiene materiales asignados en su ficha técnica."
                            : "Calculado con el stock real del inventario.");
        } catch (EntityNotFoundException error) {
            return Map.of("error", "No existe la prenda " + prendaId + ".");
        }
    }

    private Long numero(Object valor) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        if (valor instanceof String texto && !texto.isBlank()) {
            try {
                return Long.parseLong(texto.trim());
            } catch (NumberFormatException error) {
                return null;
            }
        }
        return null;
    }

    private String cadena(Object valor) {
        if (valor instanceof String texto && !texto.isBlank()) {
            return texto.trim();
        }
        return null;
    }

    /** Lo que el turno produjo: texto para mostrar e imágenes para pintar. */
    public record Respuesta(String texto, List<String> imagenes) {
    }
}
