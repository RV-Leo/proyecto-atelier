package com.atelier.backend.api;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atelier.backend.api.dto.ChatIaRequest;
import com.atelier.backend.api.dto.ChatIaResponse;
import com.atelier.backend.ia.ClienteIa;
import com.atelier.backend.ia.ClienteIa.MensajeChat;

import jakarta.validation.Valid;

/** Punto de entrada común al modelo de texto para la web y la app móvil. */
@RestController
@RequestMapping("/api/v1/ia")
public class IaController {

    private final ClienteIa clienteIa;

    public IaController(ClienteIa clienteIa) {
        this.clienteIa = clienteIa;
    }

    @PostMapping("/chat")
    public ChatIaResponse conversar(@Valid @RequestBody ChatIaRequest solicitud) {
        List<MensajeChat> mensajes = solicitud.messages().stream()
                .map(mensaje -> new MensajeChat(mensaje.role(), mensaje.content()))
                .toList();
        return new ChatIaResponse(clienteIa.conversar(mensajes));
    }
}
