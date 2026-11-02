package com.atelier.backend.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.atelier.backend.dominio.Prenda;

public interface PrendaRepository
        extends JpaRepository<Prenda, Long>, JpaSpecificationExecutor<Prenda> {

    /** La web localiza la prenda por su slug de URL, no por su id. */
    Optional<Prenda> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
