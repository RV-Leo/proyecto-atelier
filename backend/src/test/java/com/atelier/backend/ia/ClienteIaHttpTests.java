package com.atelier.backend.ia;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atelier.backend.api.dto.ViabilidadResponse;
import com.atelier.backend.config.IaProperties;
import com.atelier.backend.dominio.Prenda;
import com.atelier.backend.dominio.UnidadMedida;

/**
 * Pruebas del puente HTTP hacia un servidor de modelos compatible con OpenAI.
 *
 * <p>Es el único punto que habla con un sistema externo, y por eso se prueba con
 * el transporte simulado en lugar de con un servidor real: no se debe necesitar
 * una GPU ni bajar un modelo de 35B para verificar que el backend arma bien la
 * petición y lee bien la respuesta.
 *
 * <p>Se usan tiempos de espera en 0 para no reemplazar el transporte simulado.
 */
class ClienteIaHttpTests {

    private static final String BASE = "http://modelo.test/v1";

    @TempDir
    Path carpetaTemporal;

    @Test
    void construirPromptDevuelveElTextoDelModelo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                // El nombre del modelo configurado debe viajar en la petición.
                .andExpect(content().string(containsString("qwen-texto")))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Vestido largo de lino azul"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Vestido largo de lino azul", cliente.construirPromptDeImagen(prenda()));
        servidor.verify();
    }

    /** Un modelo que devuelve espacio en blanco no debe producir un prompt vacío. */
    @Test
    void construirPromptCaeEnPlantillaSiElModeloNoDiceNada() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"   "}}]}
                        """, MediaType.APPLICATION_JSON));

        String prompt = cliente.construirPromptDeImagen(prenda());
        assertFalse(prompt.isBlank(), "el prompt nunca puede quedar vacío");
        assertTrue(prompt.contains("vestido"), "la plantilla debe recoger el tipo de prenda");
    }

    /** Un modelo que devuelve estructura inesperada tampoco debe romper nada. */
    @Test
    void construirPromptCaeEnPlantillaSiLaRespuestaEsInesperada() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("{\"algo\":\"otra cosa\"}", MediaType.APPLICATION_JSON));

        assertFalse(cliente.construirPromptDeImagen(prenda()).isBlank());
    }

    @Test
    void explicarViabilidadDevuelveElRelatoDelModelo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Se puede producir: hay stock suficiente."}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Se puede producir: hay stock suficiente.", cliente.explicarViabilidad(contexto()));
    }

    /**
     * Un fallo al redactar el comentario no debe tirar abajo un cálculo de
     * stock que ya salió bien: se devuelve null y el servicio usa su plantilla.
     */
    @Test
    void explicarViabilidadDevuelveNullSiElServidorFalla() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withServerError());

        assertNull(cliente.explicarViabilidad(contexto()));
    }

    @Test
    void generarImagenesUsaLaUrlDevuelta() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("qwen-imagen")))
                .andRespond(withSuccess("""
                        {"data":[{"url":"http://modelo.test/img1.png"},{"url":"http://modelo.test/img2.png"}]}
                        """, MediaType.APPLICATION_JSON));

        List<ClienteIa.ImagenGenerada> imagenes = cliente.generarImagenes("un vestido", 2);

        assertEquals(2, imagenes.size());
        assertEquals("http://modelo.test/img1.png", imagenes.get(0).url());
        // El prompt queda guardado con la imagen, para poder reproducirla.
        assertEquals("un vestido", imagenes.get(0).promptUsado());
        servidor.verify();
    }

    /** Algunos servidores devuelven los bytes en base64 en vez de una URL. */
    @Test
    void generarImagenesGuardaEnDiscoLaVersionEnBase64() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        String contenido = "no es un png de verdad, solo se comprueba que se guarda";
        String base64 = Base64.getEncoder().encodeToString(contenido.getBytes());

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withSuccess("""
                        {"data":[{"b64_json":"%s"}]}
                        """.formatted(base64), MediaType.APPLICATION_JSON));

        List<ClienteIa.ImagenGenerada> imagenes = cliente.generarImagenes("un vestido", 1);

        assertEquals(1, imagenes.size());
        String url = imagenes.get(0).url();
        assertTrue(url.startsWith("/imagenes/"), "la imagen guardada debe servirse bajo /imagenes/");

        Path guardada = carpetaTemporal.resolve(url.substring("/imagenes/".length()));
        assertTrue(Files.exists(guardada), "el archivo debe existir en disco");
        assertEquals(contenido, Files.readString(guardada));
    }

    /** Un 5xx del servidor debe llegar como 503, no como 500 con stack de Spring. */
    @Test
    void generarImagenesFallaConExcepcionPropiaSiElServidorDevuelve500() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withServerError());

        assertThrows(IaNoDisponibleException.class,
                () -> cliente.generarImagenes("un vestido", 1));
    }

    /** Mismo criterio para un 4xx: la petición no es culpa del cliente del API. */
    @Test
    void generarImagenesFallaConExcepcionPropiaSiElServidorDevuelve400() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(IaNoDisponibleException.class,
                () -> cliente.generarImagenes("un vestido", 1));
    }

    @Test
    void generarImagenesFallaSiElModeloNoDevuelveImagenes() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andRespond(withSuccess("""
                        {"data":[{"texto":"esto no es una imagen"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(IaNoDisponibleException.class,
                () -> cliente.generarImagenes("un vestido", 1));
    }

    /** La cantidad se limita a 4: pedir 50 no debe traducirse en 50 imágenes. */
    @Test
    void generarImagenesLimitaLaCantidadPedida() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteIaHttp cliente = cliente(servidor, builder);

        servidor.expect(requestTo(BASE + "/images/generations"))
                .andExpect(content().string(containsString("\"n\":4")))
                .andRespond(withSuccess("""
                        {"data":[{"url":"http://modelo.test/a.png"}]}
                        """, MediaType.APPLICATION_JSON));

        cliente.generarImagenes("un vestido", 50);
        servidor.verify();
    }

    private ClienteIaHttp cliente(MockRestServiceServer servidor, RestClient.Builder builder) {
        IaProperties propiedades = new IaProperties(
                BASE, "", "qwen-texto", "qwen-imagen", 0, carpetaTemporal.toString());
        return new ClienteIaHttp(propiedades, new GuardadorImagenes(propiedades), builder);
    }

    private Prenda prenda() {
        Prenda p = new Prenda();
        p.setNombre("Vestido de verano");
        p.setTipoPrenda("vestido");
        p.setEstilo("casual");
        p.setColores("negro");
        p.setTela("viscosa");
        return p;
    }

    private ClienteIa.ContextoViabilidad contexto() {
        return new ClienteIa.ContextoViabilidad(
                prenda(),
                false,
                50,
                List.of(new ViabilidadResponse.Detalle(
                        1L, "Viscosa", UnidadMedida.METRO,
                        new BigDecimal("10"), new BigDecimal("4"), false)),
                List.of(new ViabilidadResponse.Alternativa(
                        2L, "Viscosa premium", UnidadMedida.METRO,
                        new BigDecimal("80"), "stock insuficiente: faltan 6")));
    }
}
