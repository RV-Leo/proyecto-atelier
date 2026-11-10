package com.atelier.backend.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atelier.backend.api.dto.GenerarPropuestasRequest;
import com.atelier.backend.api.dto.PromptResponse;
import com.atelier.backend.api.dto.PropuestaDisenoResponse;
import com.atelier.backend.servicio.DisenoServicio;

import jakarta.validation.Valid;

/**
 * Diseño generativo: convertir la descripción de una prenda en imágenes.
 *
 * <p>Es el módulo "crear imágenes de prendas a partir de detalles como el tipo,
 * el estilo, los colores y la tela" del README. El backend no entrena ni
 * ejecuta el modelo: lo invoca a través del puente de IA y conserva el
 * resultado.
 */
@RestController
@RequestMapping("/api/v1/prendas/{prendaId}/diseno")
public class DisenoController {

    private final DisenoServicio servicio;

    public DisenoController(DisenoServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Devuelve el prompt que se usaría, sin generar imágenes.
     *
     * <p>Permite revisar o corregir la instrucción antes de gastar tiempo de
     * GPU. Responde con {@code fuente}: MODELO si lo redactó la IA, PLANTILLA
     * si no hay servidor de modelos y se armó con una regla fija.
     */
    @GetMapping("/prompt")
    public PromptResponse construirPrompt(@PathVariable Long prendaId) {
        return servicio.construirPrompt(prendaId);
    }

    /** Genera propuestas de imagen y las guarda. */
    @PostMapping("/propuestas")
    public ResponseEntity<List<PropuestaDisenoResponse>> generar(
            @PathVariable Long prendaId,
            @Valid @RequestBody(required = false) GenerarPropuestasRequest datos) {
        GenerarPropuestasRequest peticion =
                datos == null ? new GenerarPropuestasRequest(null) : datos;
        List<PropuestaDisenoResponse> creadas =
                servicio.generarPropuestas(prendaId, peticion.cantidadEfectiva());
        return ResponseEntity
                .created(URI.create("/api/v1/prendas/" + prendaId + "/diseno/propuestas"))
                .body(creadas);
    }

    /** Propuestas ya generadas para una prenda. */
    @GetMapping("/propuestas")
    public List<PropuestaDisenoResponse> listar(@PathVariable Long prendaId) {
        return servicio.listarPropuestas(prendaId);
    }

    /**
     * Marca una propuesta como la elegida por el diseñador.
     *
     * <p>Deselecciona automáticamente las demás: solo puede haber una elegida.
     */
    @PutMapping("/propuestas/{propuestaId}/seleccion")
    public PropuestaDisenoResponse seleccionar(
            @PathVariable Long prendaId,
            @PathVariable Long propuestaId) {
        return servicio.seleccionar(propuestaId);
    }
}
