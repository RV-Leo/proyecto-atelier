package com.atelier.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.PropuestaDiseno;

public interface PropuestaDisenoRepository extends JpaRepository<PropuestaDiseno, Long> {

    List<PropuestaDiseno> findByPrendaId(Long prendaId);
}
