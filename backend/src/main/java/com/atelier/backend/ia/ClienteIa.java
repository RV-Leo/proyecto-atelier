package com.atelier.backend.ia;

import java.util.List;

import com.atelier.backend.api.dto.ViabilidadResponse;
import com.atelier.backend.dominio.Prenda;

/** Contrato del backend para comunicarse con el proveedor de IA. */
public interface ClienteIa {

    /** Indica si el proveedor está configurado. */
    boolean disponible();

    /** Convierte los datos de una prenda en un prompt de imagen. */
    String construirPromptDeImagen(Prenda prenda);

    /** Explica un resultado calculado por el backend, o devuelve {@code null}. */
    String explicarViabilidad(ContextoViabilidad contexto);

    /** Envía el historial validado al modelo. */
    String conversar(List<MensajeChat> mensajes);

    /** Genera hasta cuatro imágenes a partir del prompt. */
    List<ImagenGenerada> generarImagenes(String prompt, int cantidad);

    /** Resultado de viabilidad ya calculado por Spring Boot. */
    record ContextoViabilidad(
            Prenda prenda,
            boolean viable,
            int puntaje,
            List<ViabilidadResponse.Detalle> detalles,
            List<ViabilidadResponse.Alternativa> alternativas) {
    }

    /** Mensaje del historial de chat. */
    record MensajeChat(String role, String content) {
    }

    /** Imagen generada y prompt usado. */
    record ImagenGenerada(String url, String promptUsado) {
    }
}
