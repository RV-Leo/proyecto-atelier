package com.atelier.backend.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Historial de mensajes que se enviará al modelo de texto. */
public record ChatIaRequest(
        @NotEmpty
        @Size(max = 20)
        List<@Valid Mensaje> messages) {

    @AssertTrue(message = "El último mensaje debe ser del usuario")
    public boolean isLastMessageFromUser() {
        return messages != null
                && !messages.isEmpty()
                && "user".equals(messages.get(messages.size() - 1).role());
    }

    public record Mensaje(
            @NotBlank
            @Pattern(regexp = "user|assistant")
            String role,
            @NotBlank
            @Size(max = 4000)
            String content) {
    }
}
