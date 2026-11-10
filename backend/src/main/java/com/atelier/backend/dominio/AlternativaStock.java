package com.atelier.backend.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Alternativa sugerida cuando falta stock de un material.
 *
 * <p>M\u00f3dulo "viabilidad e inventario": "buscar alternativas cuando falte
 * stock" (README). Compara el material faltante con el propuesto.
 */
@Entity
@Table(name = "alternativa_stock")
public class AlternativaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analisis_id", nullable = false)
    private AnalisisViabilidad analisis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_alternativo_id", nullable = false)
    private Material materialAlternativo;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(name = "cantidad_sugerida", precision = 12, scale = 3)
    private BigDecimal cantidadSugerida;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AnalisisViabilidad getAnalisis() {
        return analisis;
    }

    public void setAnalisis(AnalisisViabilidad analisis) {
        this.analisis = analisis;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Material getMaterialAlternativo() {
        return materialAlternativo;
    }

    public void setMaterialAlternativo(Material materialAlternativo) {
        this.materialAlternativo = materialAlternativo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public BigDecimal getCantidadSugerida() {
        return cantidadSugerida;
    }

    public void setCantidadSugerida(BigDecimal cantidadSugerida) {
        this.cantidadSugerida = cantidadSugerida;
    }
}
