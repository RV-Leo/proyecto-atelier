package com.atelier.backend.api;

import java.util.List;
import java.util.Base64;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atelier.backend.api.dto.ChatIaRequest;
import com.atelier.backend.api.dto.ChatIaResponse;
import com.atelier.backend.api.dto.EditarImagenIaRequest;
import com.atelier.backend.ia.ClienteIa;
import com.atelier.backend.ia.ClienteIa.MensajeChat;
import com.atelier.backend.ia.agentes.OrquestadorAgentes;

import jakarta.validation.Valid;

/** Punto de entrada común al modelo de texto para la web y la app móvil. */
@RestController
@RequestMapping("/api/v1/ia")
public class IaController {

    private final ClienteIa clienteIa;
    private final OrquestadorAgentes agentes;

    public IaController(ClienteIa clienteIa, OrquestadorAgentes agentes) {
        this.clienteIa = clienteIa;
        this.agentes = agentes;
    }

    @PostMapping("/chat")
    public ChatIaResponse conversar(@Valid @RequestBody ChatIaRequest solicitud) {
        List<MensajeChat> mensajes = solicitud.messages().stream()
                .map(mensaje -> new MensajeChat(mensaje.role(), mensaje.content()))
                .toList();
        return new ChatIaResponse(agentes.conversar(mensajes, solicitud.contexto()));
    }

    @PostMapping("/images/edits")
    public Map<String, String> editarImagen(@Valid @RequestBody EditarImagenIaRequest solicitud) {
        byte[] imagen;
        try {
            imagen = Base64.getDecoder().decode(solicitud.imageBase64());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("La imagen de referencia no es válida.", error);
        }
        if (imagen.length > 8_000_000) {
            throw new IllegalArgumentException("La imagen debe pesar menos de 8 MB.");
        }
        var resultado = clienteIa.editarImagen(solicitud.prompt(), imagen, solicitud.contentType());
        return Map.of("imagenUrl", resultado.url(), "promptUsado", resultado.promptUsado());
    }
}
