package com.atelier.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.AnalisisViabilidad;

public interface AnalisisViabilidadRepository extends JpaRepository<AnalisisViabilidad, Long> {

    List<AnalisisViabilidad> findByPrendaIdOrderByAnalizadoEnDesc(Long prendaId);
}
