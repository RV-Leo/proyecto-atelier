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
import jakarta.persistence.Table;

/**
 * Una tendencia capturada de Pinterest o de una tienda de marca.
 *
 * <p>M\u00f3dulo "tendencias" del README: Europa y Am\u00e9rica, con los colores,
 * cortes y telas que la IA identifica en la imagen.
 */
@Entity
@Table(name = "tendencia")
public class Tendencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FuenteTendencia fuente;

    @Column(name = "url_origen", nullable = false, length = 500, unique = true)
    private String urlOrigen;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(length = 200)
    private String colores;

    @Column(length = 40)
    private String estilo;

    @Column(length = 80)
    private String tela;

    @Column(name = "capturado_en", nullable = false, updatable = false)
    private Instant capturadoEn;

    @PrePersist
    private void alPersistir() {
        capturadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public FuenteTendencia getFuente() {
        return fuente;
    }

    public void setFuente(FuenteTendencia fuente) {
        this.fuente = fuente;
    }

    public String getUrlOrigen() {
        return urlOrigen;
    }

    public void setUrlOrigen(String urlOrigen) {
        this.urlOrigen = urlOrigen;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getColores() {
        return colores;
    }

    public void setColores(String colores) {
        this.colores = colores;
    }

    public String getEstilo() {
        return estilo;
    }

    public void setEstilo(String estilo) {
        this.estilo = estilo;
    }

    public String getTela() {
        return tela;
    }

    public void setTela(String tela) {
        this.tela = tela;
    }

    public Instant getCapturadoEn() {
        return capturadoEn;
    }
}
