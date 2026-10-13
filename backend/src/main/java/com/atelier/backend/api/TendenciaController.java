package com.atelier.backend.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atelier.backend.api.dto.TendenciaRequest;
import com.atelier.backend.api.dto.TendenciaResponse;
import com.atelier.backend.dominio.Region;
import com.atelier.backend.servicio.TendenciaServicio;

import jakarta.validation.Valid;

/**
 * Tendencias capturadas (módulo "tendencias" del README: Europa y América).
 */
@RestController
@RequestMapping("/api/v1/tendencias")
public class TendenciaController {

    private final TendenciaServicio servicio;

    public TendenciaController(TendenciaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<TendenciaResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/region/{region}")
    public List<TendenciaResponse> porRegion(@PathVariable Region region) {
        return servicio.porRegion(region);
    }

    @PostMapping
    public ResponseEntity<TendenciaResponse> crear(@Valid @RequestBody TendenciaRequest datos) {
        TendenciaResponse creada = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/tendencias/" + creada.id())).body(creada);
    }
}
