package com.atelier.backend.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atelier.backend.api.dto.PaginaResponse;
import com.atelier.backend.api.dto.PrendaMaterialRequest;
import com.atelier.backend.api.dto.PrendaMaterialResponse;
import com.atelier.backend.api.dto.PrendaRequest;
import com.atelier.backend.api.dto.PrendaResponse;
import com.atelier.backend.dominio.EstadoPrenda;
import com.atelier.backend.dominio.Material;
import com.atelier.backend.dominio.Prenda;
import com.atelier.backend.dominio.PrendaMaterial;
import com.atelier.backend.dominio.Slug;
import com.atelier.backend.repositorio.MaterialRepository;
import com.atelier.backend.repositorio.PrendaMaterialRepository;
import com.atelier.backend.repositorio.PrendaRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;

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
    public PaginaResponse<PrendaResponse> listar(
            String q, String tipo, String estilo, EstadoPrenda estado,
            Integer pagina, Integer tamano) {
        Specification<Prenda> filtros = conFiltros(q, tipo, estilo, estado);

        if (pagina == null) {
            List<PrendaResponse> todas = prendas.findAll(filtros, Sort.by("creadoEn").descending())
                    .stream()
                    .map(PrendaResponse::desde)
                    .toList();
            return PaginaResponse.completa(todas);
        }

        // Las prendas más recientes primero, que es como las lista el catálogo.
        Pageable pageable = PaginaResponse.aPageable(
                pagina, tamano, Sort.by("creadoEn").descending());
        return PaginaResponse.desde(prendas.findAll(filtros, pageable).map(PrendaResponse::desde));
    }

    /** Mismos filtros que la web aplica en su catálogo. */
    private Specification<Prenda> conFiltros(
            String q, String tipo, String estilo, EstadoPrenda estado) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("descripcion")), patron)));
            }

            if (tipo != null && !tipo.isBlank()) {
                condiciones.add(cb.equal(cb.lower(root.get("tipoPrenda")), tipo.toLowerCase(Locale.ROOT)));
            }

            if (estilo != null && !estilo.isBlank()) {
                condiciones.add(cb.equal(cb.lower(root.get("estilo")), estilo.toLowerCase(Locale.ROOT)));
            }

            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public PrendaResponse obtener(Long id) {
        return PrendaResponse.desde(obtenerEntidad(id));
    }

    /** Búsqueda por slug, que es como la web abre el detalle de una prenda. */
    @Transactional(readOnly = true)
    public PrendaResponse obtenerPorSlug(String slug) {
        return PrendaResponse.desde(prendas.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("No existe la prenda " + slug)));
    }

    public PrendaResponse crear(PrendaRequest datos) {
        Prenda prenda = new Prenda();
        prenda.setNombre(datos.nombre());
        prenda.setSlug(slugUnico(datos.nombre(), null));
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

    /**
     * Genera un slug no repetido.
     *
     * <p>Dos prendas pueden llamarse igual, así que al segundo se le añade un
     * sufijo numérico. Si se está editando la propia prenda, su slug actual no
     * cuenta como ocupado.
     */
    private String slugUnico(String nombre, Long propioId) {
        String base = Slug.de(nombre);
        if (base.isBlank()) {
            base = "prenda";
        }
        String candidato = base;
        int sufijo = 2;
        while (prendas.findBySlug(candidato)
                .filter(p -> propioId == null || !p.getId().equals(propioId))
                .isPresent()) {
            candidato = base + "-" + sufijo++;
        }
        return candidato;
    }

    private Prenda obtenerEntidad(Long id) {
        return prendas.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la prenda " + id));
    }
}
