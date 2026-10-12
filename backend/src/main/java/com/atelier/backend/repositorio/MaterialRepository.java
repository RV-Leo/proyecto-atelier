package com.atelier.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByActivoTrue();
}
