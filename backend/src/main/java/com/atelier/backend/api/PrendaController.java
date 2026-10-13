package com.atelier.backend.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atelier.backend.api.dto.PaginaResponse;
import com.atelier.backend.api.dto.PrendaMaterialRequest;
import com.atelier.backend.api.dto.PrendaMaterialResponse;
import com.atelier.backend.api.dto.PrendaRequest;
import com.atelier.backend.api.dto.PrendaResponse;
import com.atelier.backend.dominio.EstadoPrenda;
import com.atelier.backend.servicio.PrendaServicio;

import jakarta.validation.Valid;

/**
 * Prendas del módulo de diseño generativo.
 *
 * <p>Una prenda guarda lo que el usuario describe (tipo, estilo, colores,
 * tela) y su ficha técnica, que es lo que permite responder después si se puede
 * producir.
 */
@RestController
@RequestMapping("/api/v1/prendas")
public class PrendaController {

    private final PrendaServicio servicio;

    public PrendaController(PrendaServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * Lista las prendas.
     *
     * <p>Filtros y paginación son opcionales: sin {@code pagina} se devuelve
     * todo. El catálogo de la web pagina a 12 por página.
     */
    @GetMapping
    public PaginaResponse<PrendaResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estilo,
            @RequestParam(required = false) EstadoPrenda estado,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano) {
        return servicio.listar(q, tipo, estilo, estado, pagina, tamano);
    }

    /**
     * Detalle por slug, que es como la web enlaza cada prenda
     * ({@code /catalogo/<slug>/}).
     *
     * <p>Se declara antes que {@code /{id}} para que el tramo literal gane en la
     * resolución de rutas.
     */
    @GetMapping("/slug/{slug}")
    public PrendaResponse porSlug(@PathVariable String slug) {
        return servicio.obtenerPorSlug(slug);
    }

    @GetMapping("/{id}")
    public PrendaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PrendaResponse> crear(@Valid @RequestBody PrendaRequest datos) {
        PrendaResponse creada = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/prendas/" + creada.id())).body(creada);
    }

    /** Ficha técnica: qué materiales necesita la prenda y cuánto stock hay. */
    @GetMapping("/{id}/materiales")
    public List<PrendaMaterialResponse> materiales(@PathVariable Long id) {
        return servicio.materiales(id);
    }

    /** Agrega o corrige un material de la ficha técnica de la prenda. */
    @PostMapping("/{id}/materiales")
    public ResponseEntity<PrendaMaterialResponse> asignarMaterial(
            @PathVariable Long id,
            @Valid @RequestBody PrendaMaterialRequest datos) {
        PrendaMaterialResponse creada = servicio.asignarMaterial(id, datos);
        return ResponseEntity
                .created(URI.create("/api/v1/prendas/" + id + "/materiales/" + creada.materialId()))
                .body(creada);
    }
}
