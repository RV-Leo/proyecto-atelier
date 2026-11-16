package com.atelier.backend.repositorio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.atelier.backend.dominio.Region;
import com.atelier.backend.dominio.Tendencia;

public interface TendenciaRepository extends JpaRepository<Tendencia, Long> {

    List<Tendencia> findByRegionOrderByCapturadoEnDesc(Region region);

    Page<Tendencia> findByRegion(Region region, Pageable pageable);
}
