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
import com.atelier.backend.ia.skills.SkillAtelier;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Cliente de texto de Gemini mediante la API Interactions. */
public class ClienteGemini implements ClienteIa {

    /**
     * Prompt para tareas internas (viabilidad): no necesita el alcance ni las
     * herramientas del asistente, solo describir datos ya calculados.
     */
    private static final String ROL_SISTEMA = """
            Eres Atelier IA, el asistente de moda y diseño textil. Respondes siempre en español,
            con lenguaje claro y amable. Usa los datos de catálogo recibidos en el contexto
            solo para responder sobre esos artículos. No inventes precios ni stock; si falta
            un dato, dilo con claridad.
            """;

    /**
     * Tope de vueltas modelo → herramienta → modelo. Evita que un modelo que
     * insiste en pedir la misma herramienta deje la petición HTTP colgada.
     */
    private static final int MAX_PASOS_HERRAMIENTAS = 4;

    private static final ObjectMapper JSON = new ObjectMapper();

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
        List<Map<String, Object>> historial = construirHistorial(mensajes);
        Map<?, ?> respuesta = enviar(historial, instruccion(ROL_SISTEMA, contexto), null);
        return textoRequerido(respuesta);
    }

    /**
     * Conversación con herramientas: Gemini decide cuándo consultar stock o
     * generar una imagen, y el backend ejecuta lo que pide.
     *
     * <p>Se usa el modo sin estado ({@code store: false}) porque el backend ya
     * conserva el historial de mensajes y no depende de interacciones previas.
     */
    @Override
    public String conversar(List<MensajeChat> mensajes, String contexto,
            List<Herramienta> herramientas, EjecutorHerramientas ejecutor) {
        if (!disponible()) {
            throw new IaNoDisponibleException("Configura GEMINI_API_KEY para habilitar el chat.");
        }
        if (herramientas == null || herramientas.isEmpty() || ejecutor == null) {
            return conversar(mensajes, contexto);
        }

        List<Map<String, Object>> historial = construirHistorial(mensajes);
        String sistema = instruccion(SkillAtelier.PROMPT, contexto);
        List<Map<String, Object>> declaraciones = herramientas.stream()
                .map(Herramienta::declaracion)
                .toList();

        for (int paso = 0; paso < MAX_PASOS_HERRAMIENTAS; paso++) {
            Map<?, ?> respuesta = enviar(historial, sistema, declaraciones);
            List<Map<String, Object>> llamadas = llamadasDeFuncion(respuesta);

            if (llamadas.isEmpty()) {
                return textoRequerido(respuesta);
            }

            // El modo sin estado exige devolver los pasos del modelo tal cual
            // llegaron (incluidos los function_call) antes del resultado.
            historial.addAll(pasos(respuesta));

            for (Map<String, Object> llamada : llamadas) {
                String nombre = String.valueOf(llamada.get("name"));
                Map<String, Object> argumentos = mapa(llamada.get("arguments"));
                Object resultado;
                try {
                    resultado = ejecutor.ejecutar(nombre, argumentos);
                } catch (RuntimeException error) {
                    resultado = Map.of("error", mensajeDe(error));
                }
                historial.add(resultadoDeFuncion(nombre, llamada.get("id"), resultado));
            }
        }

        throw new IaNoDisponibleException("El asistente pidió herramientas demasiadas veces seguidas.");
    }

    /** Envía una interacción y devuelve el cuerpo de la respuesta. */
    private Map<?, ?> enviar(List<Map<String, Object>> historial, String sistema,
            List<Map<String, Object>> herramientas) {
        if (historial.isEmpty()) {
            throw new IaNoDisponibleException("El chat no recibió mensajes para enviar a Gemini.");
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", propiedades.modeloTexto());
        cuerpo.put("store", false);
        cuerpo.put("input", historial);
        cuerpo.put("system_instruction", sistema);
        if (herramientas != null && !herramientas.isEmpty()) {
            cuerpo.put("tools", herramientas);
        }

        try {
            Map<?, ?> respuesta = rest.post().uri("/interactions")
                    .header("x-goog-api-key", propiedades.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve().body(Map.class);
            if (respuesta == null) {
                throw new IaNoDisponibleException("Gemini devolvió una respuesta vacía.");
            }
            return respuesta;
        } catch (RestClientException error) {
            throw new IaNoDisponibleException("Gemini no respondió correctamente.", error);
        }
    }

    private List<Map<String, Object>> construirHistorial(List<MensajeChat> mensajes) {
        List<Map<String, Object>> historial = new ArrayList<>();
        if (mensajes == null) {
            return historial;
        }
        for (MensajeChat mensaje : mensajes) {
            if (mensaje == null || mensaje.content() == null || mensaje.content().isBlank()) {
                continue;
            }
            String tipo = "assistant".equalsIgnoreCase(mensaje.role()) ? "model_output" : "user_input";
            historial.add(Map.of(
                    "type", tipo,
                    "content", List.of(Map.of("type", "text", "text", mensaje.content()))));
        }
        return historial;
    }

    private String instruccion(String base, String contexto) {
        return contexto == null || contexto.isBlank() ? base : base + "\n\n" + contexto;
    }

    /** Pasos {@code function_call} que el modelo pidió ejecutar. */
    private List<Map<String, Object>> llamadasDeFuncion(Map<?, ?> respuesta) {
        List<Map<String, Object>> llamadas = new ArrayList<>();
        for (Map<String, Object> paso : pasos(respuesta)) {
            if ("function_call".equals(paso.get("type"))) {
                llamadas.add(paso);
            }
        }
        return llamadas;
    }

    /** Los pasos de la respuesta como mapas serializables. */
    private List<Map<String, Object>> pasos(Map<?, ?> respuesta) {
        List<Map<String, Object>> pasos = new ArrayList<>();
        if (respuesta.get("steps") instanceof List<?> lista) {
            for (Object paso : lista) {
                Map<String, Object> mapa = mapa(paso);
                if (!mapa.isEmpty()) {
                    pasos.add(mapa);
                }
            }
        }
        return pasos;
    }

    private Map<String, Object> resultadoDeFuncion(String nombre, Object id, Object resultado) {
        Map<String, Object> paso = new LinkedHashMap<>();
        paso.put("type", "function_result");
        paso.put("name", nombre);
        paso.put("call_id", String.valueOf(id));
        paso.put("result", List.of(Map.of("type", "text", "text", json(resultado))));
        return paso;
    }

    private String textoRequerido(Map<?, ?> respuesta) {
        String texto = extraerTexto(respuesta);
        if (texto == null || texto.isBlank()) {
            throw new IaNoDisponibleException("Gemini devolvió una respuesta vacía.");
        }
        return texto.trim();
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> contenido
                ? (Map<String, Object>) contenido
                : Map.of();
    }

    private String json(Object valor) {
        try {
            return JSON.writeValueAsString(valor);
        } catch (JacksonException error) {
            return String.valueOf(valor);
        }
    }

    private String mensajeDe(RuntimeException error) {
        String mensaje = error.getMessage();
        return mensaje == null || mensaje.isBlank()
                ? "La herramienta falló sin detalle."
                : mensaje;
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
