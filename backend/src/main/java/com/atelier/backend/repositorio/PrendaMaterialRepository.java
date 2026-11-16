package com.atelier.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.PrendaMaterial;

public interface PrendaMaterialRepository extends JpaRepository<PrendaMaterial, Long> {

    List<PrendaMaterial> findByPrendaId(Long prendaId);

    Optional<PrendaMaterial> findByPrendaIdAndMaterialId(Long prendaId, Long materialId);
}
