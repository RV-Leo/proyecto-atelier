package com.atelier.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.AlternativaStock;

public interface AlternativaStockRepository extends JpaRepository<AlternativaStock, Long> {

    List<AlternativaStock> findByAnalisisId(Long analisisId);
}
