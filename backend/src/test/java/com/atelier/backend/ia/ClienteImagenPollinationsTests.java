package com.atelier.backend.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atelier.backend.config.IaProperties;

/**
 * Pruebas del generador de imágenes gratuito de Pollinations.
 *
 * <p>Se prueban aquí las tres peculiaridades que lo separan de un proveedor
 * compatible con OpenAI: pide por GET con el prompt en la ruta, devuelve los
 * bytes de la foto en vez de un JSON, y <strong>nunca</strong> debe recibir la
 * cabecera Authorization porque es un proveedor distinto del de texto.
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

        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/prompt/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));

        ClienteIa.ImagenGenerada imagen = cliente.generar("un vestido de lino", 1).get(0);

        // La imagen debe servirse bajo /imagenes/ y con la extensión real.
        assertTrue(imagen.url().startsWith("/imagenes/"), imagen.url());
        Path guardada = carpetaTemporal.resolve(imagen.url().substring("/imagenes/".length()));
        assertTrue(Files.exists(guardada), "el archivo debe existir en disco");
        assertTrue(guardada.toString().endsWith(".jpg"), "un JPEG no puede llamarse .png");
        assertEquals(JPEG.length, Files.size(guardada));
        servidor.verify();
    }

    /**
     * El prompt viaja codificado dentro de la ruta: un espacio en blanco no
     * puede partir la URL en dos.
     */
    @Test
    void codificaElPromptDentroDeLaRuta() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(org.hamcrest.Matchers.containsString(
                        "/prompt/un%20vestido%20de%20lino%20color%20terracota")))
                .andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));

        cliente.generar("un vestido de lino color terracota", 1);
        servidor.verify();
    }

    /**
     * Pollinations es un proveedor externo distinto al de texto: mandarle la
     * clave de Google sería filtrarla a un tercero.
     */
    @Test
    void nuncaEnviaAuthorization() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/prompt/")))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));

        cliente.generar("una blusa", 1);
        servidor.verify();
    }

    /** Un 5xx debe llegar como IaNoDisponibleException (503), no como 500. */
    @Test
    void convierteLosErroresDelServidorEnExcepcionPropia() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/prompt/")))
                .andRespond(withServerError());

        assertThrows(IaNoDisponibleException.class, () -> cliente.generar("una blusa", 1));
    }

    /** Una respuesta vacía no es una imagen: hay que rechazarla. */
    @Test
    void rechazaLasImagenesVacias() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/prompt/")))
                .andRespond(withSuccess(new byte[0], MediaType.IMAGE_JPEG));

        assertThrows(IaNoDisponibleException.class, () -> cliente.generar("una blusa", 1));
    }

    /**
     * El 402 es el límite gratuito por IP, no una avería: si ya hay una imagen
     * se devuelve el resultado parcial en lugar de fallar.
     */
    @Test
    void devuelveLoObtenidoCuandoElLimiteDeRitmoAparecePorMItad() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteImagenPollinations cliente = cliente(builder);

        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/prompt/")))
                .andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/prompt/")))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED)
                        .header(HttpHeaders.RETRY_AFTER, "45")
                        .body("""
                                {"status":402,"error":{"code":"FREE_LIMIT"}}"""));

        var imagenes = cliente.generar("una blusa", 3);

        assertEquals(1, imagenes.size(), "con una imagen ya entregada no se falla");
        servidor.verify();
    }

    private ClienteImagenPollinations cliente(RestClient.Builder builder) {
        IaProperties propiedades = new IaProperties(
                "https://gemini.test/v1", "", "gemini-texto", "", 0,
                carpetaTemporal.toString(), BASE);
        return new ClienteImagenPollinations(propiedades, new GuardadorImagenes(propiedades), builder);
    }
}
