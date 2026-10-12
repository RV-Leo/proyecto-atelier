package com.atelier.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.PrendaMaterial;

public interface PrendaMaterialRepository extends JpaRepository<PrendaMaterial, Long> {

    List<PrendaMaterial> findByPrendaId(Long prendaId);
}
