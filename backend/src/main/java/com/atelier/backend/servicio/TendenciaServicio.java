package com.atelier.backend.servicio;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atelier.backend.api.dto.PaginaResponse;
import com.atelier.backend.api.dto.TendenciaRequest;
import com.atelier.backend.api.dto.TendenciaResponse;
import com.atelier.backend.dominio.Tendencia;
import com.atelier.backend.repositorio.TendenciaRepository;

/**
 * Tendencias capturadas de Pinterest y tiendas de marca.
 *
 * <p>El campo {@code url_origen} es único a propósito: cada tendencia se guarda
 * una sola vez aunque dos rastreos distintas la encuentren.
 */
@Service
@Transactional
public class TendenciaServicio {

    private final TendenciaRepository tendencias;

    public TendenciaServicio(TendenciaRepository tendencias) {
        this.tendencias = tendencias;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<TendenciaResponse> listar(Integer pagina, Integer tamano) {
        if (pagina == null) {
            return PaginaResponse.completa(
                    tendencias.findAll(Sort.by("capturadoEn").descending())
                            .stream()
                            .map(TendenciaResponse::desde)
                            .toList());
        }
        return PaginaResponse.desde(tendencias
                .findAll(PaginaResponse.aPageable(pagina, tamano, Sort.by("capturadoEn").descending()))
                .map(TendenciaResponse::desde));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<TendenciaResponse> porRegion(
            com.atelier.backend.dominio.Region region, Integer pagina, Integer tamano) {
        if (pagina == null) {
            return PaginaResponse.completa(
                    tendencias.findByRegionOrderByCapturadoEnDesc(region)
                            .stream()
                            .map(TendenciaResponse::desde)
                            .toList());
        }
        return PaginaResponse.desde(tendencias
                .findByRegion(region, PaginaResponse.aPageable(
                        pagina, tamano, Sort.by("capturadoEn").descending()))
                .map(TendenciaResponse::desde));
    }

    public TendenciaResponse crear(TendenciaRequest datos) {
        Tendencia tendencia = new Tendencia();
        tendencia.setTitulo(datos.titulo());
        tendencia.setRegion(datos.region());
        tendencia.setFuente(datos.fuente());
        tendencia.setUrlOrigen(datos.urlOrigen());
        tendencia.setImagenUrl(datos.imagenUrl());
        tendencia.setColores(datos.colores());
        tendencia.setEstilo(datos.estilo());
        tendencia.setTela(datos.tela());

        try {
            return TendenciaResponse.desde(tendencias.save(tendencia));
        } catch (DataIntegrityViolationException e) {
            // La restricción única de url_origen no traduce bien sola: avisamos
            // que esa tendencia ya estaba registrada.
            throw new IllegalArgumentException("La tendencia de esa URL ya fue registrada", e);
        }
    }
}
