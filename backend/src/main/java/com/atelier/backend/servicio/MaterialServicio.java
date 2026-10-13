package com.atelier.backend.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atelier.backend.api.dto.MaterialRequest;
import com.atelier.backend.api.dto.MaterialResponse;
import com.atelier.backend.api.dto.PaginaResponse;
import com.atelier.backend.dominio.EstadoMaterial;
import com.atelier.backend.dominio.Material;
import com.atelier.backend.repositorio.MaterialRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

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
    public PaginaResponse<MaterialResponse> listar(
            String q, EstadoMaterial estado, Boolean activo, Integer pagina, Integer tamano) {
        Specification<Material> filtros = conFiltros(q, estado, activo);

        // Sin parámetros de paginación se devuelve todo, para las vistas que hoy
        // cargan el inventario completo (inventario y panel de la web).
        if (pagina == null) {
            List<MaterialResponse> todos = materiales.findAll(filtros, Sort.by("nombre"))
                    .stream()
                    .map(MaterialResponse::desde)
                    .toList();
            return PaginaResponse.completa(todos);
        }

        return PaginaResponse.desde(
                materiales.findAll(filtros, PaginaResponse.aPageable(pagina, tamano))
                        .map(MaterialResponse::desde));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MaterialResponse> listarActivos(Integer pagina, Integer tamano) {
        return listar(null, null, true, pagina, tamano);
    }

    /**
     * Combina los filtros opcionales.
     *
     * <p>La búsqueda libre cubre nombre, tipo y color, que son los campos que la
     * web dejaba filtrar en su inventario.
     */
    private Specification<Material> conFiltros(String q, EstadoMaterial estado, Boolean activo) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("tipo")), patron),
                        cb.like(cb.lower(root.get("color")), patron)));
            }

            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }

            if (estado != null) {
                Path<BigDecimal> stock = root.get("stock");
                Path<BigDecimal> minimo = root.get("stockMinimo");
                switch (estado) {
                    // Los umbrales tienen que coincidir con EstadoMaterial.de, si no
                    // el filtro devolveria materiales cuyo estado calculado es otro.
                    case AGOTADO -> condiciones.add(
                            cb.lessThanOrEqualTo(stock, BigDecimal.ZERO));
                    case BAJO -> condiciones.add(cb.and(
                            cb.greaterThan(stock, BigDecimal.ZERO),
                            cb.lessThanOrEqualTo(stock, minimo)));
                    case DISPONIBLE -> condiciones.add(cb.greaterThan(stock, minimo));
                }
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
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
