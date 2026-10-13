package com.atelier.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comportamiento del puente de IA sin servidor de modelos configurado.
 *
 * <p>Es el estado por defecto del proyecto: el README quiere usar Qwen, pero
 * mientras ese servidor no exista la API tiene que seguir siendo usable. Estas
 * pruebas fijan ese contrato:
 *
 * <ul>
 *   <li>el prompt se arma con plantilla y lo dice ({@code fuente: PLANTILLA});</li>
 *   <li>pedir imágenes devuelve 503, que es "el proveedor no está", no "tu
 *       petición está mal";</li>
 *   <li>la viabilidad se sigue calculando igual, con el comentario de plantilla.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PuenteIaTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.security.user.name}")
    private String usuario;

    @Value("${spring.security.user.password}")
    private String clave;

    @Test
    void promptSeArmaConPlantillaCuandoNoHayIa() throws Exception {
        int prenda = crearPrenda();

        mockMvc.perform(get("/api/v1/prendas/" + prenda + "/diseno/prompt")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").isNotEmpty())
                .andExpect(jsonPath("$.fuente").value("PLANTILLA"));
    }

    /** El prompt de plantilla recoge los datos que describió el diseñador. */
    @Test
    void promptDePlantillaIncluyeLosDatosDeLaPrenda() throws Exception {
        int prenda = crearPrenda();

        String cuerpo = mockMvc.perform(get("/api/v1/prendas/" + prenda + "/diseno/prompt")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String prompt = extraerCampo(cuerpo, "prompt");
        org.hamcrest.MatcherAssert.assertThat(prompt,
                org.hamcrest.Matchers.containsString("vestido"));
        org.hamcrest.MatcherAssert.assertThat(prompt,
                org.hamcrest.Matchers.containsString("viscosa"));
    }

    @Test
    void generarPropuestasSinIaResponde503() throws Exception {
        int prenda = crearPrenda();

        mockMvc.perform(post("/api/v1/prendas/" + prenda + "/diseno/propuestas")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":2}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void prendaInexistenteEnDisenoResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/prendas/999999/diseno/prompt")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/prendas/999999/diseno/propuestas")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    /**
     * El cálculo de viabilidad no depende de la IA: sigue dando el mismo
     * veredicto y el comentario de plantilla.
     */
    @Test
    void viabilidadFuncionaSinIaYUsaComentarioDePlantilla() throws Exception {
        int tela = crearMaterial("Lino", "tela", "crudo", "200");
        int prenda = crearPrenda();

        mockMvc.perform(post("/api/v1/prendas/" + prenda + "/materiales")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materialId":%d,"cantidadRequerida":"15"}
                                """.formatted(tela)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/viabilidad/" + prenda).with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viable").value(true))
                .andExpect(jsonPath("$.puntaje").value(100))
                .andExpect(jsonPath("$.comentario").isNotEmpty());
    }

    /** La viabilidad de una prenda sin materiales no se rompe sin IA. */
    @Test
    void viabilidadSinMaterialesNoFallaSinIa() throws Exception {
        int prenda = crearPrenda();

        mockMvc.perform(post("/api/v1/viabilidad/" + prenda).with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viable").value(false))
                .andExpect(jsonPath("$.puntaje").value(0))
                .andExpect(jsonPath("$.comentario").isNotEmpty());
    }

    /** Seleccionar una propuesta inexistente responde 404, no 500. */
    @Test
    void seleccionarPropuestaInexistenteResponde404() throws Exception {
        int prenda = crearPrenda();

        mockMvc.perform(put("/api/v1/prendas/" + prenda + "/diseno/propuestas/999999/seleccion")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isNotFound());
    }

    /** Lee un valor textual del cuerpo JSON, sin depender de un parser. */
    private String extraerCampo(String json, String campo) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + campo + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private int crearPrenda() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/prendas")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Vestido de verano","tipoPrenda":"vestido",
                                 "estilo":"casual","colores":"negro","tela":"viscosa"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Integer.parseInt(respuesta.replaceAll("(?s).*\"id\":\\s*(\\d+).*", "$1").trim());
    }

    private int crearMaterial(String nombre, String tipo, String color, String stock) throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/materiales")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","tipo":"%s","color":"%s","unidad":"METRO",
                                 "stock":"%s","stockMinimo":"1","activo":true}
                                """.formatted(nombre, tipo, color, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Integer.parseInt(respuesta.replaceAll("(?s).*\"id\":\\s*(\\d+).*", "$1").trim());
    }
}
