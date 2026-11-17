package com.atelier.backend.ia;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.atelier.backend.config.IaProperties;
import com.atelier.backend.dominio.Prenda;

/** Cliente de texto de Gemini mediante la API Interactions. */
public class ClienteGemini implements ClienteIa {

    private static final String ROL_SISTEMA = """
            Eres Atelier IA, el asistente de moda y diseño textil. Respondes siempre en español,
            con lenguaje claro y amable. Usa los datos de catálogo recibidos en el contexto
            solo para responder sobre esos artículos. No inventes precios ni stock; si falta
            un dato, dilo con claridad.
            """;

    private final IaProperties propiedades;
    private final RestClient rest;

    public ClienteGemini(IaProperties propiedades, RestClient.Builder builder) {
        this.propiedades = propiedades;
        RestClient.Builder configurado = builder.baseUrl(propiedades.url())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (propiedades.timeoutSegundos() > 0) {
            Duration espera = Duration.ofSeconds(propiedades.timeoutSegundos());
            SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
            fabrica.setConnectTimeout(espera);
            fabrica.setReadTimeout(espera);
            configurado.requestFactory(fabrica);
        }
        this.rest = configurado.build();
    }

    @Override
    public boolean disponible() {
        return propiedades.habilitada()
                && propiedades.apiKey() != null
                && !propiedades.apiKey().isBlank();
    }

    @Override
    public String conversar(List<MensajeChat> mensajes) {
        return conversar(mensajes, null);
    }

    @Override
    public String conversar(List<MensajeChat> mensajes, String contexto) {
        if (!disponible()) {
            throw new IaNoDisponibleException("Configura GEMINI_API_KEY para habilitar el chat.");
        }

        List<Map<String, Object>> input = new ArrayList<>();
        for (MensajeChat mensaje : mensajes) {
            if (mensaje == null || mensaje.content() == null || mensaje.content().isBlank()) {
                continue;
            }
            String tipo = "assistant".equalsIgnoreCase(mensaje.role()) ? "model_output" : "user_input";
            input.add(Map.of(
                    "type", tipo,
                    "content", List.of(Map.of("type", "text", "text", mensaje.content()))));
        }
        if (input.isEmpty()) {
            throw new IaNoDisponibleException("El chat no recibió mensajes para enviar a Gemini.");
        }

        String instruccion = ROL_SISTEMA;
        if (contexto != null && !contexto.isBlank()) {
            instruccion += "\n\n" + contexto;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", propiedades.modeloTexto());
        cuerpo.put("input", input);
        cuerpo.put("system_instruction", instruccion);

        try {
            Map<?, ?> respuesta = rest.post().uri("/interactions")
                    .header("x-goog-api-key", propiedades.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve().body(Map.class);
            String texto = extraerTexto(respuesta);
            if (texto == null || texto.isBlank()) {
                throw new IaNoDisponibleException("Gemini devolvió una respuesta vacía.");
            }
            return texto.trim();
        } catch (RestClientException error) {
            throw new IaNoDisponibleException("Gemini no respondió correctamente.", error);
        }
    }

    private String extraerTexto(Map<?, ?> respuesta) {
        if (respuesta == null || !(respuesta.get("steps") instanceof List<?> pasos)) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        for (Object paso : pasos) {
            if (!(paso instanceof Map<?, ?> mapaPaso)
                    || !"model_output".equals(mapaPaso.get("type"))
                    || !(mapaPaso.get("content") instanceof List<?> partes)) {
                continue;
            }
            for (Object parte : partes) {
                if (parte instanceof Map<?, ?> mapaParte
                        && "text".equals(mapaParte.get("type"))
                        && mapaParte.get("text") instanceof String fragmento) {
                    texto.append(fragmento);
                }
            }
        }
        return texto.toString();
    }

    @Override
    public String construirPromptDeImagen(Prenda prenda) {
        // No llamar a Gemini para reformular este prompt: las solicitudes de
        // imagen quedan completamente en Pollinations y no gastan tokens Gemini.
        return "Fotografía editorial de producto de moda, prenda completa centrada, "
                + "acabado realista, fondo de estudio limpio e iluminación suave. "
                + "Prenda: " + texto(prenda.getNombre())
                + ". Tipo: " + texto(prenda.getTipoPrenda())
                + ". Estilo: " + texto(prenda.getEstilo())
                + ". Colores: " + texto(prenda.getColores())
                + ". Tela: " + texto(prenda.getTela())
                + ". Detalles: " + texto(prenda.getDescripcion())
                + ". Sin texto, marcas ni logotipos.";
    }

    @Override
    public String explicarViabilidad(ContextoViabilidad contexto) {
        StringBuilder datos = new StringBuilder();
        datos.append("Prenda: ").append(texto(contexto.prenda().getNombre())).append('\n');
        datos.append("Resultado: ").append(contexto.viable() ? "viable" : "no viable")
                .append(" (cobertura ").append(contexto.puntaje()).append("%)\n");
        datos.append("Materiales:\n");
        contexto.detalles().forEach(detalle -> datos.append("- ").append(detalle.material())
                .append(": requerido ").append(detalle.requerido()).append(' ')
                .append(detalle.unidad()).append(", disponible ").append(detalle.disponible())
                .append(detalle.suficiente() ? " (suficiente)\n" : " (insuficiente)\n"));
        if (!contexto.alternativas().isEmpty()) {
            datos.append("Alternativas:\n");
            contexto.alternativas().forEach(alternativa -> datos.append("- ")
                    .append(alternativa.material()).append(": ").append(alternativa.motivo()).append('\n'));
        }
        String pregunta = "Explica en máximo 4 frases, en español y sin tecnicismos, si se puede "
                + "producir esta prenda, qué material falta y qué alternativa sirve. Interpreta "
                + "el resultado sin repetir las cifras.\n\n" + datos;
        try {
            return conversar(List.of(new MensajeChat("user", pregunta)));
        } catch (IaNoDisponibleException error) {
            return null;
        }
    }

    @Override
    public List<ImagenGenerada> generarImagenes(String prompt, int cantidad) {
        throw new IaNoDisponibleException("Las imágenes se generan con el cliente de Pollinations.");
    }

    @Override
    public ImagenGenerada editarImagen(String prompt, byte[] imagen, String contentType) {
        throw new IaNoDisponibleException("Las imágenes se editan con el cliente de Pollinations.");
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "(sin indicar)" : valor;
    }
}
