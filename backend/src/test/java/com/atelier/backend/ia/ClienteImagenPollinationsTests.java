package com.atelier.backend.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atelier.backend.config.IaProperties;

/**
 * Pruebas del generador de imágenes de Pollinations.
 *
 * <p>Pollinations usa un protocolo propio: {@code POST /images/generations}
 * con su propia clave y devuelve las imágenes en base64 dentro de un JSON.
 * Estas pruebas fijan ese contrato y, sobre todo, que la clave de Gemini nunca
 * viaje al proveedor de imágenes.
 */
class ClienteImagenPollinationsTests {

    private static final String BASE = "https://gen.pollinations.ai/v1";
    private static final String CLAVE_IMAGEN = "pollinations-test";
    private static final String CLAVE_TEXTO = "gemini-secreta";

    /** Firma mágica de un JPEG: FF D8 FF. */
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    private static final String JPEG_BASE64 = Base64.getEncoder().encodeToString(JPEG);

    @TempDir
    Path carpetaTemporal;

    @Test
    void guardaLaImagenDevueltaComoBytes() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(cuerpoCon(JPEG_BASE64), MediaType.APPLICATION_JSON));

        ClienteIa.ImagenGenerada imagen = cliente.generar("un vestido de lino", 1).get(0);

        // La imagen debe servirse bajo /imagenes/ y con la extensión real.
        assertTrue(imagen.url().startsWith("/imagenes/"), imagen.url());
        Path guardada = carpetaTemporal.resolve(imagen.url().substring("/imagenes/".length()));
        assertTrue(Files.exists(guardada), "el archivo debe existir en disco");
        assertTrue(guardada.toString().endsWith(".jpg"), "un JPEG no puede llamarse .png");
        assertEquals(JPEG.length, Files.size(guardada));
        servidor.verify();
    }

    /** El prompt viaja en el cuerpo JSON, no en la ruta. */
    @Test
    void enviaElPromptEnElCuerpo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(content().string(Matchers.containsString("un vestido de lino color terracota")))
                .andRespond(withSuccess(cuerpoCon(JPEG_BASE64), MediaType.APPLICATION_JSON));

        cliente.generar("un vestido de lino color terracota", 1);
        servidor.verify();
    }

    /**
     * Pollinations es un proveedor externo distinto al de texto: solo recibe su
     * propia clave, nunca la de Google.
     */
    @Test
    void enviaSoloLaClaveDePollinations() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CLAVE_IMAGEN))
                .andExpect(content().string(Matchers.not(Matchers.containsString(CLAVE_TEXTO))))
                .andRespond(withSuccess(cuerpoCon(JPEG_BASE64), MediaType.APPLICATION_JSON));

        cliente.generar("una blusa", 1);
        servidor.verify();
    }

    /** Un 5xx debe llegar como IaNoDisponibleException (503), no como 500. */
    @Test
    void convierteLosErroresDelServidorEnExcepcionPropia() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withServerError());

        assertThrows(IaNoDisponibleException.class, () -> cliente.generar("una blusa", 1));
    }

    /** Una respuesta sin imágenes no es una imagen: hay que rechazarla. */
    @Test
    void rechazaLasRespuestasSinImagenes() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(IaNoDisponibleException.class, () -> cliente.generar("una blusa", 1));
    }

    /** Todas las imágenes que llegan en la respuesta se guardan. */
    @Test
    void guardaTodasLasImagenesDeLaRespuesta() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        String dos = "{\"data\":[{\"b64_json\":\"" + JPEG_BASE64
                + "\"},{\"b64_json\":\"" + JPEG_BASE64 + "\"}]}";
        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withSuccess(dos, MediaType.APPLICATION_JSON));

        var imagenes = cliente.generar("una blusa", 2);

        assertEquals(2, imagenes.size());
        assertFalse(imagenes.get(0).url().equals(imagenes.get(1).url()),
                "cada propuesta debe tener su propio archivo");
        servidor.verify();
    }

    private String cuerpoCon(String base64) {
        return "{\"data\":[{\"b64_json\":\"" + base64 + "\"}]}";
    }

    private ClienteImagenPollinations cliente(RestClient.Builder builder) {
        IaProperties propiedades = new IaProperties(
                "https://gemini.test/v1", CLAVE_TEXTO, "gemini-texto", "modelo-imagen",
                0, carpetaTemporal.toString(), BASE, CLAVE_IMAGEN);
        return new ClienteImagenPollinations(propiedades, new GuardadorImagenes(propiedades), builder);
    }
}
