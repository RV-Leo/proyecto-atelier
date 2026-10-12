package com.atelier.backend.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.Prenda;

public interface PrendaRepository extends JpaRepository<Prenda, Long> {
}
