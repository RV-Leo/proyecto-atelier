package com.atelier.backend.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.atelier.backend.config.IaProperties;
import com.atelier.backend.ia.ClienteIa.MensajeChat;
import com.atelier.backend.ia.skills.CatalogoHerramientas;

/** Bucle de herramientas de Gemini sobre la API Interactions. */
class ClienteGeminiTests {

    private static final String BASE = "https://gemini.test/v1";

    @TempDir
    Path carpetaTemporal;

    /** Sin función pedida, devuelve el texto del modelo tal cual. */
    @Test
    void devuelveElTextoCuandoNoPideHerramientas() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteGemini cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/interactions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"steps":[{"type":"model_output","content":[{"type":"text","text":"Hola, soy Atelier."}]}]}
                        """, MediaType.APPLICATION_JSON));

        String respuesta = cliente.conversar(
                List.of(new MensajeChat("user", "hola")),
                null,
                CatalogoHerramientas.todas(),
                (nombre, argumentos) -> Map.of("ok", true));

        assertEquals("Hola, soy Atelier.", respuesta);
        servidor.verify();
    }

    /**
     * Si el modelo pide {@code consultar_stock}, el backend ejecuta y devuelve
     * el resultado, y la respuesta final es el texto del segundo turno.
     */
    @Test
    void ejecutaLaHerramientaYDevuelveElResultadoAlModelo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteGemini cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/interactions"))
                .andExpect(content().string(Matchers.containsString("\"tools\"")))
                .andExpect(content().string(Matchers.containsString("consultar_stock")))
                .andRespond(withSuccess("""
                        {"status":"requires_action","steps":[
                          {"type":"function_call","name":"consultar_stock",
                           "arguments":{"prenda_id":7},"id":"call_1"}]}
                        """, MediaType.APPLICATION_JSON));

        servidor.expect(requestTo(BASE + "/interactions"))
                .andExpect(content().string(Matchers.containsString("\"function_result\"")))
                .andExpect(content().string(Matchers.containsString("\"call_id\":\"call_1\"")))
                .andExpect(content().string(Matchers.containsString("alcanza")))
                .andRespond(withSuccess("""
                        {"steps":[{"type":"model_output","content":[
                          {"type":"text","text":"Sí alcanza: hay lino suficiente."}]}]}
                        """, MediaType.APPLICATION_JSON));

        AtomicReference<String> herramienta = new AtomicReference<>();
        AtomicReference<Map<String, Object>> argumentos = new AtomicReference<>();

        String respuesta = cliente.conversar(
                List.of(new MensajeChat("user", "¿puedo producir la prenda 7?")),
                null,
                CatalogoHerramientas.todas(),
                (nombre, datos) -> {
                    herramienta.set(nombre);
                    argumentos.set(datos);
                    return Map.of("materiales", List.of(), "nota", "alcanza");
                });

        assertEquals("consultar_stock", herramienta.get());
        assertEquals(7, ((Number) argumentos.get().get("prenda_id")).intValue());
        assertEquals("Sí alcanza: hay lino suficiente.", respuesta);
        servidor.verify();
    }

    /** El prompt de negocio viaja como instrucción de sistema. */
    @Test
    void enviaElPromptDeNegocioComoInstruccionDeSistema() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteGemini cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/interactions"))
                .andExpect(content().string(Matchers.containsString("asistente de diseño textil de Atelier")))
                .andExpect(content().string(Matchers.containsString("fuera de ese alcance")))
                .andRespond(withSuccess("""
                        {"steps":[{"type":"model_output","content":[{"type":"text","text":"Listo."}]}]}
                        """, MediaType.APPLICATION_JSON));

        cliente.conversar(
                List.of(new MensajeChat("user", "ayúdame con un vestido")),
                null,
                CatalogoHerramientas.todas(),
                (nombre, argumentos) -> Map.of());

        servidor.verify();
    }

    /** Un error de la herramienta vuelve al modelo en lugar de cortar el chat. */
    @Test
    void informaAlModeloCuandoLaHerramientaFalla() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ClienteGemini cliente = cliente(builder);

        servidor.expect(requestTo(BASE + "/interactions"))
                .andRespond(withSuccess("""
                        {"status":"requires_action","steps":[
                          {"type":"function_call","name":"generar_imagen",
                           "arguments":{"prenda_id":99,"instruccion":"un vestido"},"id":"call_9"}]}
                        """, MediaType.APPLICATION_JSON));

        servidor.expect(requestTo(BASE + "/interactions"))
                .andExpect(content().string(Matchers.containsString("No existe la prenda 99")))
                .andRespond(withSuccess("""
                        {"steps":[{"type":"model_output","content":[
                          {"type":"text","text":"No encuentro esa prenda."}]}]}
                        """, MediaType.APPLICATION_JSON));

        String respuesta = cliente.conversar(
                List.of(new MensajeChat("user", "genera el vestido 99")),
                null,
                CatalogoHerramientas.todas(),
                (nombre, argumentos) -> {
                    throw new IllegalStateException("No existe la prenda 99");
                });

        assertTrue(respuesta.contains("No encuentro"));
        servidor.verify();
    }

    private ClienteGemini cliente(RestClient.Builder builder) {
        IaProperties propiedades = new IaProperties(
                BASE, "clave-gemini", "gemini-test", "modelo-imagen",
                0, carpetaTemporal.toString(), "", "");
        return new ClienteGemini(propiedades, builder);
    }
}
