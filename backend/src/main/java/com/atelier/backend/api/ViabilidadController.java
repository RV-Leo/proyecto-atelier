package com.atelier.backend.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atelier.backend.api.dto.PrendaMaterialResponse;
import com.atelier.backend.api.dto.ViabilidadResponse;
import com.atelier.backend.servicio.ViabilidadServicio;

/**
 * Análisis de viabilidad de una prenda contra el stock real.
 *
 * <p>Es el corazón del módulo: "¿puedo producir esta prenda?". El servicio
 * compara la ficha técnica con el inventario, guarda el veredicto y sugiere
 * alternativas para lo que falte.
 */
@RestController
@RequestMapping("/api/v1/viabilidad")
public class ViabilidadController {

    private final ViabilidadServicio servicio;

    public ViabilidadController(ViabilidadServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Analiza la prenda y guarda el resultado.
     *
     * <p>Devuelve 200 aunque el veredicto sea "no viable": el análisis es una
     * respuesta válida, no un error.
     */
    @PostMapping("/{prendaId}")
    public ViabilidadResponse analizar(@PathVariable Long prendaId) {
        return servicio.analizar(prendaId);
    }

    /** Ficha técnica de la prenda con el estado de cada material. */
    @GetMapping("/{prendaId}/materiales")
    public List<PrendaMaterialResponse> fichaTecnica(@PathVariable Long prendaId) {
        return servicio.fichaTecnica(prendaId);
    }
}
