package com.atelier.backend.ia;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.atelier.backend.config.IaProperties;
import com.atelier.backend.dominio.Prenda;

/**
 * Puente real hacia un servidor de inferencia compatible con OpenAI.
 *
 * <p>Casi todos los servidores locales que ejecutan modelos Qwen (vLLM, Ollama,
 * LM Studio, TGI) exponen esta misma API. Por eso el backend no depende de un
 * proveedor concreto: cambiando la URL sirve para cualquiera, o para el
 * gateway de Django si el equipo decide que ese sea el intermediario.
 *
 * <p>Las respuestas del modelo son texto libre y poco confiable en su formato,
 * así que este componente nunca es la fuente de verdad de los datos: Spring
 * Boot calcula el stock y la viabilidad, y el modelo solo redacta. Si el modelo
 * falla, se propaga {@link IaNoDisponibleException} y el servicio correspondiente
 * usa su vía determinista.
 *
 * <p>No lleva {@code @Component}: lo registra {@link ConfigIa} según haya URL
 * de servidor o no.
 */
public class ClienteIaHttp implements ClienteIa {

    private static final Logger log = LoggerFactory.getLogger(ClienteIaHttp.class);

    private static final String ROL_SISTEMA =
            "Eres el asistente de diseño textil de Atelier, una aplicación para rubro textil. "
                    + "Respondes siempre en español, con lenguaje claro para diseñadores. "
                    + "No inventes datos de inventario: si te dan cifras, usa exactamente esas.";

    private final IaProperties propiedades;
    private final RestClient rest;
    private final GuardadorImagenes guardador;

    /**
     * @param builder inyectado en lugar de creado aquí, para que las pruebas
     *                puedan sustituir el transporte con {@code MockRestServiceServer}
     *                y ejercitar este cliente sin levantar un servidor de modelos.
     */
    public ClienteIaHttp(IaProperties propiedades, GuardadorImagenes guardador, RestClient.Builder builder) {
        this.propiedades = propiedades;
        this.guardador = guardador;

        // Sin tiempo de espera, un servidor de modelos colgado dejaría un hilo
        // de Tomcat bloqueado para siempre. Generar imágenes es lento, por eso
        // el límite es configurable y no un valor fijo. Un valor <= 0 no impone
        // límite, lo que además deja el transporte libre para las pruebas.
        RestClient.Builder configurado = builder
                .baseUrl(propiedades.url())
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
        return propiedades.habilitada();
    }

    @Override
    public String construirPromptDeImagen(Prenda prenda) {
        String instruccion = """
                Convierte esta idea de prenda en una instrucción en español para un modelo \
                que genera imágenes de moda. Describe la prenda, el corte, los colores y la \
                tela. Sé concreto y visual. No agregues marcas ni logos. Solo devuelve la \
                instrucción, sin comillas ni explicaciones.

                Nombre: %s
                Tipo: %s
                Estilo: %s
                Colores: %s
                Tela: %s
                Descripción: %s
                """.formatted(
                texto(prenda.getNombre()),
                texto(prenda.getTipoPrenda()),
                texto(prenda.getEstilo()),
                texto(prenda.getColores()),
                texto(prenda.getTela()),
                texto(prenda.getDescripcion()));

        String respuesta = chat(instruccion);
        if (respuesta == null || respuesta.isBlank()) {
            // Mejor una instrucción básica que ninguna.
            return instruccionBase(prenda);
        }
        return respuesta.trim();
    }

    @Override
    public String explicarViabilidad(ContextoViabilidad contexto) {
        StringBuilder datos = new StringBuilder();
        datos.append("Prenda: ").append(contexto.prenda().getNombre()).append('\n');
        datos.append("Resultado: ").append(contexto.viable() ? "viable" : "no viable").append('\n');
        datos.append("Cobertura de materiales: ").append(contexto.puntaje()).append("%\n\n");

        datos.append("Materiales:\n");
        contexto.detalles().forEach(d -> datos.append("- ").append(d.material()).append(": se requieren ")
                .append(d.requerido()).append(' ').append(d.unidad())
                .append(", hay ").append(d.disponible())
                .append(d.suficiente() ? " (suficiente)\n" : " (insuficiente)\n"));

        if (!contexto.alternativas().isEmpty()) {
            datos.append("\nAlternativas sugeridas:\n");
            contexto.alternativas().forEach(a -> datos.append("- ").append(a.material())
                    .append(": ").append(a.motivo()).append('\n'));
        }

        String instruccion = """
                Explica en máximo 4 frases, en español y sin tecnicismos, si esta prenda se \
                puede producir y por qué. Menciona qué material falta si falta alguno y para \
                qué sirve la alternativa propuesta. No repitas las cifras una por una, \
                interpreta el resultado.

                %s
                """.formatted(datos);

        String respuesta = chat(instruccion);
        return respuesta == null || respuesta.isBlank() ? null : respuesta.trim();
    }

    @Override
    public List<ImagenGenerada> generarImagenes(String prompt, int cantidad) {
        int pedidas = Math.max(1, Math.min(cantidad, 4));

        Map<String, Object> cuerpo = Map.of(
                "model", propiedades.modeloImagen(),
                "prompt", prompt,
                "n", pedidas,
                "size", "1024x1024");

        Map<?, ?> respuesta;
        try {
            respuesta = rest.post()
                    .uri("/images/generations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            // Se captura RestClientException y no solo ResourceAccessException:
            // un 4xx o 5xx del servidor también es "no pude generar", y tendría
            // que llegar como 503, no como 500 con el stack de Spring.
            throw new IaNoDisponibleException(
                    "El servidor de imágenes no respondió correctamente en " + propiedades.url()
                            + ": " + e.getMessage(),
                    e);
        }

        if (respuesta == null || !(respuesta.get("data") instanceof List<?> datos)) {
            throw new IaNoDisponibleException(
                    "El servidor de imágenes no devolvió ninguna imagen (respuesta inesperada)");
        }

        List<ImagenGenerada> generadas = new ArrayList<>();
        for (Object elemento : datos) {
            if (!(elemento instanceof Map<?, ?> item)) {
                continue;
            }
            Object url = item.get("url");
            Object base64 = item.get("b64_json");

            if (url instanceof String u && !u.isBlank()) {
                generadas.add(new ImagenGenerada(u, prompt));
            } else if (base64 instanceof String b && !b.isBlank()) {
                // Algunos servidores devuelven la imagen codificada en vez de una
                // URL; hay que guardarla para poder servirla.
                generadas.add(new ImagenGenerada(guardador.guardar(b), prompt));
            }
        }

        if (generadas.isEmpty()) {
            throw new IaNoDisponibleException(
                    "El servidor de imágenes respondió pero sin imágenes utilizables");
        }
        return generadas;
    }

    /**
     * Llama al modelo de texto y devuelve su respuesta, o {@code null} si algo
     * sale mal.
     *
     * <p>Devolver {@code null} en vez de lanzar es deliberado: aquí el modelo es
     * un adorno, no una dependencia. Un fallo al redactar un comentario no debe
     * tirar abajo un cálculo de stock que ya salió bien.
     */
    private String chat(String instruccionUsuario) {
        if (!disponible()) {
            return null;
        }

        Map<String, Object> cuerpo = Map.of(
                "model", propiedades.modeloTexto(),
                "messages", List.of(
                        Map.of("role", "system", "content", ROL_SISTEMA),
                        Map.of("role", "user", "content", instruccionUsuario)),
                "temperature", 0.4);

        try {
            Map<?, ?> respuesta = rest.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);

            if (respuesta == null
                    || !(respuesta.get("choices") instanceof List<?> opciones)
                    || opciones.isEmpty()
                    || !(opciones.get(0) instanceof Map<?, ?> opcion)
                    || !(opcion.get("message") instanceof Map<?, ?> mensaje)) {
                return null;
            }
            Object contenido = mensaje.get("content");
            return contenido instanceof String c ? c : null;

        } catch (ResourceAccessException e) {
            // El servidor de texto no está. Se avisa por log y se sigue sin IA.
            log.warn("Servidor de IA no disponible en {}: {}", propiedades.url(), e.getMessage());
            return null;
        } catch (RuntimeException e) {
            log.warn("El servidor de IA devolvió un error: {}", e.getMessage());
            return null;
        }
    }

    /** Instrucción de respaldo cuando el modelo de texto no contesta. */
    private String instruccionBase(Prenda prenda) {
        StringBuilder sb = new StringBuilder("Diseño de ");
        sb.append(texto(prenda.getTipoPrenda()));
        if (prenda.getEstilo() != null && !prenda.getEstilo().isBlank()) {
            sb.append(", estilo ").append(prenda.getEstilo());
        }
        if (prenda.getColores() != null && !prenda.getColores().isBlank()) {
            sb.append(", en ").append(prenda.getColores());
        }
        if (prenda.getTela() != null && !prenda.getTela().isBlank()) {
            sb.append(", tela ").append(prenda.getTela());
        }
        return sb.append(". Fondo neutro, iluminación de estudio, prenda centrada.").toString();
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "(sin indicar)" : valor;
    }
}
