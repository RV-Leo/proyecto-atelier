package com.atelier.backend.ia;

import java.util.List;

import com.atelier.backend.dominio.Prenda;

/**
 * Puente de IA desactivado: no hay servidor de modelos configurado.
 *
 * <p>Es el comportamiento por defecto mientras el equipo no levante Qwen. El
 * backend sigue respondiendo y calculando viabilidad; lo único que cambia es
 * que el texto viene de plantillas en lugar del modelo. Así se puede usar y
 * probar la API completa sin tener que instalar primero un modelo de 35B.
 *
 * <p>Se activa solo cuando {@code atelier.ia.url} está vacía, y se apaga solo
 * cuando alguien la define.
 *
 * <p>No lleva {@code @Component}: lo registra {@link ConfigIa} según haya URL
 * de servidor o no.
 */
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
        // null a propósito: el servicio usará su comentario determinista.
        return null;
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
