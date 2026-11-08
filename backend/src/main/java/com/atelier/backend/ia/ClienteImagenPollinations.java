package com.atelier.backend.ia;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.atelier.backend.config.IaProperties;

/**
 * Generador de imÃ¡genes gratuito de Pollinations.ai.
 *
 * <p>No habla el protocolo de OpenAI, asÃ­ que no puede reutilizar
 * {@link ClienteIaHttp}: no hay {@code POST /images/generations} ni JSON de
 * respuesta. La peticiÃ³n es un {@code GET /prompt/{instrucciÃ³n}} y la respuesta
 * son los bytes de la foto, que van directos a {@link GuardadorImagenes}.
 *
 * <p>Su contrato de uso gratuito condiciona este cliente:
 * <ul>
 *   <li><b>No lleva cabecera Authorization</b> y no debe llevarla: es un
 *       proveedor distinto al de texto, y mandarle la clave de Google serÃ­a
 *       filtrarla a un tercero.</li>
 *   <li><b>Una imagen cada 45 segundos por IP.</b> Al superarlo responde
 *       {@code 402} con la cabecera {@code Retry-After}. Si ya se tiene alguna
 *       imagen se devuelve el resultado parcial en vez de fallar; solo se
 *       espera una vez, y Ãºnicamente cuando todavÃ­a no hay ninguna.</li>
 *   <li>Como mÃ¡ximo 768x768 pÃ­xeles de Ã¡rea.</li>
 * </ul>
 *
 * <p>No lleva {@code @Component}: lo registra {@link ConfigIa} cuando existe
 * {@code atelier.ia.url-imagen}.
 */
public class ClienteImagenPollinations implements GeneradorImagenes {

    private static final Logger log = LoggerFactory.getLogger(ClienteImagenPollinations.class);

    private static final int ANCHO = 768;
    private static final int ALTO = 768;

    /** LÃ­mite duro de espera en una peticiÃ³n HTTP; por debajo del timeout. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 45;

    private final IaProperties propiedades;
    private final GuardadorImagenes guardador;
    private final RestClient rest;

    public ClienteImagenPollinations(
            IaProperties propiedades,
            GuardadorImagenes guardador,
            RestClient.Builder builder) {
        this.propiedades = propiedades;
        this.guardador = guardador;

        // Sin Authorization ni Content-Type: solo pedimos bytes por GET.
        RestClient.Builder configurado = builder.baseUrl(propiedades.urlImagen());

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
    public List<ClienteIa.ImagenGenerada> generar(String prompt, int cantidad) {
        int pedidas = Math.max(1, Math.min(cantidad, 4));
        // Semilla distinta por llamada: la misma URL en Pollinations devuelve
        // siempre la misma foto, y de nada servirÃ­a pedir dos propuestas iguales.
        long semillaBase = ThreadLocalRandom.current().nextLong(1, 1_000_000);

        List<ClienteIa.ImagenGenerada> generadas = new ArrayList<>();
        boolean yaEspero = false;

        for (int indice = 0; indice < pedidas; indice++) {
            try {
                generadas.add(descargar(prompt, semillaBase + indice));
            } catch (LimiteSuperadoException limite) {
                if (!generadas.isEmpty()) {
                    // Ya hay algo que enseÃ±ar: el lÃ­mite es solo de ritmo, no
                    // un error del proyecto. Se entrega lo obtenido.
                    log.warn("Pollinations limitÃ³ el ritmo; se devuelven {} de {} propuestas",
                            generadas.size(), pedidas);
                    return generadas;
                }
                if (yaEspero) {
                    throw new IaNoDisponibleException(
                            "Pollinations agotÃ³ su lÃ­mite gratuito de imÃ¡genes: "
                                    + limite.getMessage(),
                            limite);
                }
                // Sin ninguna imagen todavÃ­a merece la pena esperar el hueco.
                yaEspero = true;
                esperar(limite.segundosEspera());
                indice--;
            }
        }

        if (generadas.isEmpty()) {
            throw new IaNoDisponibleException("Pollinations no devolviÃ³ ninguna imagen");
        }
        return generadas;
    }

    /** Descarga una imagen y la guarda en disco. */
    private ClienteIa.ImagenGenerada descargar(String prompt, long semilla) {
        URI destino = URI.create(
                propiedades.urlImagen() + "/prompt/" + codificar(prompt)
                        + "?width=" + ANCHO
                        + "&height=" + ALTO
                        + "&seed=" + semilla
                        + "&nologo=true");

        byte[] bytes;
        try {
            bytes = rest.get().uri(destino).retrieve().body(byte[].class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.PAYMENT_REQUIRED.value()) {
                // 402: es el lÃ­mite gratuito por IP, no una cacharra del servicio.
                throw new LimiteSuperadoException(segundosDeReintento(e), e);
            }
            throw new IaNoDisponibleException(
                    "Pollinations respondiÃ³ " + e.getStatusCode() + " en " + destino, e);
        } catch (ResourceAccessException e) {
            throw new IaNoDisponibleException(
                    "No se pudo contactar con Pollinations en " + propiedades.urlImagen(), e);
        } catch (RestClientException e) {
            // Un 5xx tambiÃ©n es "no pude generar", y debe llegar como 503.
            throw new IaNoDisponibleException(
                    "Pollinations no respondiÃ³ correctamente en " + propiedades.urlImagen()
                            + ": " + e.getMessage(),
                    e);
        }

        if (bytes == null || bytes.length == 0) {
            throw new IaNoDisponibleException("Pollinations devolviÃ³ una imagen vacÃ­a");
        }
        return new ClienteIa.ImagenGenerada(guardador.guardar(bytes), prompt);
    }

    /**
     * Codifica la instrucciÃ³n para llevarla dentro de la ruta.
     *
     * <p>{@code URLEncoder} pensÃ³ para query strings y deja los espacios como
     * {@code +}, que en una ruta es un signo mÃ¡s literal: hay que reemplazarlo.
     */
    private String codificar(String prompt) {
        return URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private int segundosDeReintento(HttpClientErrorException e) {
        if (e.getResponseHeaders() == null) {
            return ESPERA_MAXIMA_SEGUNDOS;
        }
        String cabecera = e.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (cabecera == null || cabecera.isBlank()) {
            return ESPERA_MAXIMA_SEGUNDOS;
        }
        try {
            return Math.max(1, Math.min(Integer.parseInt(cabecera.trim()), ESPERA_MAXIMA_SEGUNDOS));
        } catch (NumberFormatException formato) {
            return ESPERA_MAXIMA_SEGUNDOS;
        }
    }

    /**
     * Espera el hueco que pide el proveedor.
     *
     * <p>Dormir un hilo de Tomcat no es lo ideal, pero el lÃ­mite es de 45 s,
     * el timeout del puente es configurable y solo se espera cuando todavÃ­a no
     * hay ninguna imagen que entregar.
     */
    private void esperar(int segundos) {
        log.info("Pollinations pide esperar {} s antes de la siguiente imagen", segundos);
        try {
            Thread.sleep(Duration.ofSeconds(segundos));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaNoDisponibleException("Se interrumpiÃ³ la espera de Pollinations", e);
        }
    }

    /** SeÃ±al interna: el proveedor pide esperar, no estÃ¡ caÃ­do. */
    private static final class LimiteSuperadoException extends RuntimeException {

        private final int segundosEspera;

        private LimiteSuperadoException(int segundosEspera, Throwable causa) {
            super(causa.getMessage(), causa);
            this.segundosEspera = segundosEspera;
        }

        private int segundosEspera() {
            return segundosEspera;
        }
    }
}
