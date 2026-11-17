package com.atelier.backend.ia;

import java.util.List;

import com.atelier.backend.dominio.Prenda;

/** Implementación de respaldo cuando no hay proveedor de IA configurado. */
public class ClienteIaNoDisponible implements ClienteIa {

    @Override
    public boolean disponible() {
        return false;
    }

    @Override
    public String construirPromptDeImagen(Prenda prenda) {
        StringBuilder sb = new StringBuilder("Diseño de ");
        sb.append(vacio(prenda.getTipoPrenda()) ? "prenda" : prenda.getTipoPrenda());
        if (!vacio(prenda.getEstilo())) {
            sb.append(", estilo ").append(prenda.getEstilo());
        }
        if (!vacio(prenda.getColores())) {
            sb.append(", en ").append(prenda.getColores());
        }
        if (!vacio(prenda.getTela())) {
            sb.append(", tela ").append(prenda.getTela());
        }
        return sb.append(". Fondo neutro, iluminación de estudio, prenda centrada.").toString();
    }

    @Override
    public String explicarViabilidad(ContextoViabilidad contexto) {
        return null;
    }

    @Override
    public String conversar(List<MensajeChat> mensajes) {
        throw new IaNoDisponibleException(
                "La IA no está configurada. Define IA_URL y IA_API_KEY para habilitarla.");
    }

    @Override
    public List<ImagenGenerada> generarImagenes(String prompt, int cantidad) {
        throw new IaNoDisponibleException(
                "La IA no está configurada. Define atelier.ia.url para habilitarla.");
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
