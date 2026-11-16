package com.atelier.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.atelier.backend.dominio.Material;

/**
 * Inventario de materiales.
 *
 * <p>Hereda de {@code JpaSpecificationExecutor} porque la web combina filtros
 * opcionales (búsqueda libre, estado de stock, activo) que cambian en cada
 * petición: con métodos derivados habría que declarar un método por combinación.
 */
public interface MaterialRepository
        extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {

    List<Material> findByActivoTrue();
}
