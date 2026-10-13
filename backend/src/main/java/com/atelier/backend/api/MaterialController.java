package com.atelier.backend.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atelier.backend.api.dto.MaterialRequest;
import com.atelier.backend.api.dto.MaterialResponse;
import com.atelier.backend.servicio.MaterialServicio;

import jakarta.validation.Valid;

/**
 * Inventario de materiales (módulo "viabilidad e inventario").
 *
 * <p>Es el stock real contra el que se comparan las ideas de prenda. El Django
 * de la web y la app movil deben leerlo desde aquí, no desde sus propias
 * tablas.
 */
@RestController
@RequestMapping("/api/v1/materiales")
public class MaterialController {

    private final MaterialServicio servicio;

    public MaterialController(MaterialServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<MaterialResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/activos")
    public List<MaterialResponse> listarActivos() {
        return servicio.listarActivos();
    }

    @GetMapping("/{id}")
    public MaterialResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> crear(@Valid @RequestBody MaterialRequest datos) {
        MaterialResponse creado = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/materiales/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public MaterialResponse actualizar(@PathVariable Long id, @Valid @RequestBody MaterialRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
