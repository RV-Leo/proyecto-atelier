package com.atelier.backend.servicio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atelier.backend.api.dto.MaterialRequest;
import com.atelier.backend.api.dto.MaterialResponse;
import com.atelier.backend.dominio.Material;
import com.atelier.backend.repositorio.MaterialRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Inventario de materiales.
 *
 * <p>Módulo "viabilidad e inventario" del README: mantener el stock al día es
 * lo que permite responder si una prenda se puede producir.
 */
@Service
@Transactional
public class MaterialServicio {

    private final MaterialRepository materiales;

    public MaterialServicio(MaterialRepository materiales) {
        this.materiales = materiales;
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listar() {
        return materiales.findAll().stream().map(MaterialResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listarActivos() {
        return materiales.findByActivoTrue().stream().map(MaterialResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse obtener(Long id) {
        return MaterialResponse.desde(obtenerEntidad(id));
    }

    public MaterialResponse crear(MaterialRequest datos) {
        Material material = new Material();
        aplicar(datos, material);
        return MaterialResponse.desde(materiales.save(material));
    }

    public MaterialResponse actualizar(Long id, MaterialRequest datos) {
        aplicar(datos, obtenerEntidad(id));
        return MaterialResponse.desde(materiales.save(obtenerEntidad(id)));
    }

    public void eliminar(Long id) {
        materiales.delete(obtenerEntidad(id));
    }

    /**
     * Descuenta stock si hay material suficiente.
     *
     * @return {@code true} si el descuento se aplicó; {@code false} si no hay
     *         stock suficiente (no se modifica nada)
     */
    public boolean descontarStock(Long materialId, BigDecimal cantidad) {
        Material material = obtenerEntidad(materialId);
        if (material.getStock().compareTo(cantidad) < 0) {
            return false;
        }
        material.setStock(material.getStock().subtract(cantidad));
        materiales.save(material);
        return true;
    }

    private Material obtenerEntidad(Long id) {
        return materiales.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el material " + id));
    }

    private void aplicar(MaterialRequest datos, Material material) {
        material.setNombre(datos.nombre());
        material.setTipo(datos.tipo());
        material.setColor(datos.color());
        material.setUnidad(datos.unidad());
        material.setStock(datos.stock());
        material.setStockMinimo(datos.stockMinimo());
        material.setProveedor(datos.proveedor());
        material.setPrecioUnitario(datos.precioUnitario());
        material.setActivo(datos.activo() == null || datos.activo());
    }
}
