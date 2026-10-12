package com.atelier.backend.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Una prenda dise\u00f1ada por el usuario y generada por IA.
 *
 * <p>M\u00f3dulo "dise\u00f1o generativo" del README: tipo, estilo, colores y tela
 * describen la prenda; el prompt es la instrucci\u00f3n final que recibe el modelo.
 */
@Entity
@Table(name = "prenda")
public class Prenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "tipo_prenda", nullable = false, length = 40)
    private String tipoPrenda;

    @Column(nullable = false, length = 40)
    private String estilo;

    @Column(length = 200)
    private String colores;

    @Column(length = 80)
    private String tela;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "prompt_ia", columnDefinition = "text")
    private String promptIa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPrenda estado = EstadoPrenda.BORRADOR;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    private void alPersistir() {
        creadoEn = Instant.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    private void alActualizar() {
        actualizadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipoPrenda() {
        return tipoPrenda;
    }

    public void setTipoPrenda(String tipoPrenda) {
        this.tipoPrenda = tipoPrenda;
    }

    public String getEstilo() {
        return estilo;
    }

    public void setEstilo(String estilo) {
        this.estilo = estilo;
    }

    public String getColores() {
        return colores;
    }

    public void setColores(String colores) {
        this.colores = colores;
    }

    public String getTela() {
        return tela;
    }

    public void setTela(String tela) {
        this.tela = tela;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPromptIa() {
        return promptIa;
    }

    public void setPromptIa(String promptIa) {
        this.promptIa = promptIa;
    }

    public EstadoPrenda getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrenda estado) {
        this.estado = estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
