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
import jakarta.persistence.UniqueConstraint;

/**
 * Materiales que necesita una prenda, y en qu\u00e9 cantidad, para producirse.
 *
 * <p>Es la ficha t\u00e9cnica que une el dise\u00f1o con el inventario; sin ella no
 * se puede responder "iqu\u00e9 material falta?".
 */
@Entity
@Table(
        name = "prenda_material",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_prenda_material",
                columnNames = {"prenda_id", "material_id"}))
public class PrendaMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private Prenda prenda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "cantidad_requerida", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadRequerida;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prenda getPrenda() {
        return prenda;
    }

    public void setPrenda(Prenda prenda) {
        this.prenda = prenda;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public BigDecimal getCantidadRequerida() {
        return cantidadRequerida;
    }

    public void setCantidadRequerida(BigDecimal cantidadRequerida) {
        this.cantidadRequerida = cantidadRequerida;
    }
}
