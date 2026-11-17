package com.atelier.backend.ia;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.atelier.backend.config.IaProperties;

/** Cliente de imágenes de Pollinations: generación y edición sin usar Gemini. */
public class ClienteImagenPollinations implements GeneradorImagenes {

    private static final int MAX_CANTIDAD = 4;

    private final IaProperties propiedades;
    private final GuardadorImagenes guardador;
    private final RestClient rest;

    public ClienteImagenPollinations(
            IaProperties propiedades,
            GuardadorImagenes guardador,
            RestClient.Builder builder) {
        this.propiedades = propiedades;
        this.guardador = guardador;
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
        verificarConfiguracion();
        int pedidas = Math.max(1, Math.min(cantidad, MAX_CANTIDAD));
        Map<String, Object> cuerpo = Map.of(
                "model", propiedades.modeloImagen(),
                "prompt", prompt,
                "n", pedidas,
                "response_format", "b64_json",
                "size", "1024x1024");
        try {
            Map<?, ?> respuesta = rest.post().uri("/images/generations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.apiKeyImagen())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo).retrieve().body(Map.class);
            if (respuesta == null || !(respuesta.get("data") instanceof List<?> datos)) {
                throw new IaNoDisponibleException("Pollinations no devolvió imágenes utilizables.");
            }
            List<ClienteIa.ImagenGenerada> resultado = new ArrayList<>();
            for (Object item : datos) {
                if (item instanceof Map<?, ?> mapa
                        && mapa.get("b64_json") instanceof String base64
                        && !base64.isBlank()) {
                    resultado.add(new ClienteIa.ImagenGenerada(guardador.guardar(base64), prompt));
                }
            }
            if (resultado.isEmpty()) {
                throw new IaNoDisponibleException("Pollinations respondió sin imágenes codificadas.");
            }
            return resultado;
        } catch (RestClientException error) {
            throw new IaNoDisponibleException("Pollinations no pudo generar la imagen.", error);
        }
    }

    @Override
    public ClienteIa.ImagenGenerada editar(String prompt, byte[] imagen, String contentType) {
        verificarConfiguracion();
        String tipo = contentType != null && contentType.matches("image/(jpeg|png|webp)")
                ? contentType : "image/png";
        ByteArrayResource archivo = new ByteArrayResource(imagen) {
            @Override
            public String getFilename() {
                return tipo.endsWith("jpeg") ? "referencia.jpg"
                        : tipo.endsWith("webp") ? "referencia.webp" : "referencia.png";
            }
        };
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("model", propiedades.modeloImagen());
        cuerpo.add("prompt", prompt);
        HttpHeaders archivoHeaders = new HttpHeaders();
        archivoHeaders.setContentType(MediaType.parseMediaType(tipo));
        cuerpo.add("image", new HttpEntity<>(archivo, archivoHeaders));
        try {
            Map<?, ?> respuesta = rest.post().uri("/images/edits")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.apiKeyImagen())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(cuerpo).retrieve().body(Map.class);
            if (respuesta == null || !(respuesta.get("data") instanceof List<?> datos)
                    || datos.isEmpty() || !(datos.get(0) instanceof Map<?, ?> item)
                    || !(item.get("b64_json") instanceof String base64) || base64.isBlank()) {
                throw new IaNoDisponibleException("Pollinations no devolvió la imagen editada.");
            }
            return new ClienteIa.ImagenGenerada(guardador.guardar(base64), prompt);
        } catch (RestClientException error) {
            throw new IaNoDisponibleException("Pollinations no pudo editar la imagen.", error);
        }
    }

    private void verificarConfiguracion() {
        if (propiedades.apiKeyImagen() == null || propiedades.apiKeyImagen().isBlank()) {
            throw new IaNoDisponibleException("Configura POLLINATIONS_API_KEY para usar imágenes.");
        }
    }
}
