package com.atelier.backend.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Resultado de comparar una idea de prenda contra el stock real.
 *
 * <p>El README lo describe as\u00ed: "Qwen3.6 identifica los materiales necesarios
 * y Spring Boot revisa si hay stock"; aqu\u00ed se guarda ese veredicto.
 */
@Entity
@Table(name = "analisis_viabilidad")
public class AnalisisViabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private Prenda prenda;

    @Column(nullable = false)
    private boolean viable;

    /** 0 a 100; {@code null} cuando el an\u00e1lisis no devuelve puntaje. */
    private Integer puntaje;

    @Column(columnDefinition = "text")
    private String comentario;

    @Column(name = "analizado_en", nullable = false, updatable = false)
    private Instant analizadoEn;

    @PrePersist
    private void alPersistir() {
        analizadoEn = Instant.now();
    }

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

    public boolean isViable() {
        return viable;
    }

    public void setViable(boolean viable) {
        this.viable = viable;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Instant getAnalizadoEn() {
        return analizadoEn;
    }
}
