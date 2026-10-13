package com.atelier.backend.servicio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atelier.backend.api.dto.PrendaMaterialRequest;
import com.atelier.backend.api.dto.PrendaMaterialResponse;
import com.atelier.backend.api.dto.PrendaRequest;
import com.atelier.backend.api.dto.PrendaResponse;
import com.atelier.backend.dominio.EstadoPrenda;
import com.atelier.backend.dominio.Material;
import com.atelier.backend.dominio.Prenda;
import com.atelier.backend.dominio.PrendaMaterial;
import com.atelier.backend.repositorio.MaterialRepository;
import com.atelier.backend.repositorio.PrendaMaterialRepository;
import com.atelier.backend.repositorio.PrendaRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Prendas del módulo de diseño generativo.
 *
 * <p>Una prenda nace en estado {@link EstadoPrenda#BORRADOR}; su ficha técnica
 * (materiales y cantidades) se va cargando a medida que la IA propone el
 * diseño. Con esa ficha, {@link ViabilidadServicio} responde si se puede
 * producir.
 */
@Service
@Transactional
public class PrendaServicio {

    private final PrendaRepository prendas;
    private final PrendaMaterialRepository fichaTecnica;
    private final MaterialRepository materiales;

    public PrendaServicio(
            PrendaRepository prendas,
            PrendaMaterialRepository fichaTecnica,
            MaterialRepository materiales) {
        this.prendas = prendas;
        this.fichaTecnica = fichaTecnica;
        this.materiales = materiales;
    }

    @Transactional(readOnly = true)
    public List<PrendaResponse> listar() {
        return prendas.findAll().stream().map(PrendaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PrendaResponse obtener(Long id) {
        return PrendaResponse.desde(obtenerEntidad(id));
    }

    public PrendaResponse crear(PrendaRequest datos) {
        Prenda prenda = new Prenda();
        prenda.setNombre(datos.nombre());
        prenda.setTipoPrenda(datos.tipoPrenda());
        prenda.setEstilo(datos.estilo());
        prenda.setColores(datos.colores());
        prenda.setTela(datos.tela());
        prenda.setDescripcion(datos.descripcion());
        prenda.setEstado(EstadoPrenda.BORRADOR);
        return PrendaResponse.desde(prendas.save(prenda));
    }

    /** Cambia el estado del ciclo de vida de la prenda. */
    public PrendaResponse cambiarEstado(Long id, EstadoPrenda estado) {
        Prenda prenda = obtenerEntidad(id);
        prenda.setEstado(estado);
        return PrendaResponse.desde(prendas.save(prenda));
    }

    /**
     * Agrega o reemplaza un material en la ficha técnica.
     *
     * <p>Si el material ya estaba cargado, se actualiza la cantidad en lugar de
     * duplicar la fila (la tabla tiene una restricción única por par
     * prenda/material).
     */
    public PrendaMaterialResponse asignarMaterial(Long prendaId, PrendaMaterialRequest datos) {
        Prenda prenda = obtenerEntidad(prendaId);
        Material material = materiales.findById(datos.materialId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe el material " + datos.materialId()));

        PrendaMaterial fila = fichaTecnica
                .findByPrendaIdAndMaterialId(prendaId, material.getId())
                .orElseGet(() -> {
                    PrendaMaterial nueva = new PrendaMaterial();
                    nueva.setPrenda(prenda);
                    nueva.setMaterial(material);
                    return nueva;
                });
        fila.setCantidadRequerida(datos.cantidadRequerida());

        PrendaMaterial guardada = fichaTecnica.save(fila);
        return aRespuesta(guardada);
    }

    /** Ficha técnica completa de la prenda. */
    @Transactional(readOnly = true)
    public List<PrendaMaterialResponse> materiales(Long prendaId) {
        if (!prendas.existsById(prendaId)) {
            throw new EntityNotFoundException("No existe la prenda " + prendaId);
        }
        return fichaTecnica.findByPrendaId(prendaId).stream().map(this::aRespuesta).toList();
    }

    public void quitarMaterial(Long prendaId, Long materialId) {
        PrendaMaterial fila = fichaTecnica
                .findByPrendaIdAndMaterialId(prendaId, materialId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La prenda " + prendaId + " no usa el material " + materialId));
        fichaTecnica.delete(fila);
    }

    private PrendaMaterialResponse aRespuesta(PrendaMaterial fila) {
        Material material = fila.getMaterial();
        BigDecimal requerido = fila.getCantidadRequerida();
        return new PrendaMaterialResponse(
                fila.getId(),
                material.getId(),
                material.getNombre(),
                material.getUnidad(),
                requerido,
                material.getStock(),
                material.getStock().compareTo(requerido) >= 0);
    }

    private Prenda obtenerEntidad(Long id) {
        return prendas.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la prenda " + id));
    }
}
