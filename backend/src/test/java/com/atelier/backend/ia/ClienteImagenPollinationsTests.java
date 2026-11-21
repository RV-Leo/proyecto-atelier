package com.atelier.backend.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atelier.backend.config.IaProperties;

/**
 * Pruebas del generador de imágenes gratuito de Pollinations.
 *
 * <p>Se valida el contrato actual: POST OpenAI-compatible con la clave propia
 * de Pollinations y una imagen codificada en la respuesta JSON.
 */
class ClienteImagenPollinationsTests {

    private static final String BASE = "https://image.pollinations.ai";

    /** Firma mágica de un JPEG: FF D8 FF. */
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @TempDir
    Path carpetaTemporal;

    @Test
    void guardaLaImagenDevueltaComoBytes() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConImagen(), MediaType.APPLICATION_JSON));

        ClienteIa.ImagenGenerada imagen = cliente.generar("un vestido de lino", 1).get(0);

        // La imagen debe servirse bajo /imagenes/ y con la extensión real.
        assertTrue(imagen.url().startsWith("/imagenes/"), imagen.url());
        Path guardada = carpetaTemporal.resolve(imagen.url().substring("/imagenes/".length()));
        assertTrue(Files.exists(guardada), "el archivo debe existir en disco");
        assertTrue(guardada.toString().endsWith(".jpg"), "un JPEG no puede llamarse .png");
        assertEquals(JPEG.length, Files.size(guardada));
        servidor.verify();
    }

    /** El prompt viaja en el cuerpo JSON y no se pierde al serializarlo. */
    @Test
    void enviaElPromptEnElCuerpoJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "un vestido de lino color terracota")))
                .andRespond(withSuccess(respuestaConImagen(), MediaType.APPLICATION_JSON));

        cliente.generar("un vestido de lino color terracota", 1);
        servidor.verify();
    }

    /** Pollinations usa su clave de imagen y no la clave de Gemini. */
    @Test
    void usaLaClaveDePollinationsEnLaAutorizacion() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-pollinations-key"))
                .andRespond(withSuccess(respuestaConImagen(), MediaType.APPLICATION_JSON));

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

    /** Una respuesta vacía no es una imagen: hay que rechazarla. */
    @Test
    void rechazaLasImagenesVacias() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(IaNoDisponibleException.class, () -> cliente.generar("una blusa", 1));
    }

    /** El proveedor reporta el límite de uso como error y el cliente lo traduce. */
    @Test
    void convierteElLimiteDeUsoEnErrorDeServicioIa() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED)
                        .header(HttpHeaders.RETRY_AFTER, "45")
                        .body("""
                                {"status":402,"error":{"code":"FREE_LIMIT"}}"""));

        assertThrows(IaNoDisponibleException.class, () -> cliente.generar("una blusa", 1));
        servidor.verify();
    }

    private String respuestaConImagen() {
        return "{\"data\":[{\"b64_json\":\""
                + Base64.getEncoder().encodeToString(JPEG) + "\"}]}";
    }

    private ClienteImagenPollinations cliente(RestClient.Builder builder) {
        IaProperties propiedades = new IaProperties(
                "https://gemini.test/v1", "", "gemini-texto", "", 0,
                carpetaTemporal.toString(), BASE, "test-pollinations-key");
        return new ClienteImagenPollinations(propiedades, new GuardadorImagenes(propiedades), builder);
    }
}
