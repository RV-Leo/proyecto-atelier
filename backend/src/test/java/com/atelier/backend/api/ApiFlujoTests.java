package com.atelier.backend.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Pruebas de integración del flujo principal de la API contra PostgreSQL real.
 *
 * <p>La clase es {@code @Transactional}: cada prueba arranca una transacción que
 * se revierte al terminar, así no deja datos en la base de desarrollo.
 *
 * <p>Las credenciales se leen de la configuración en lugar de escribirse a
 * mano, para que el test sirva igual en otra máquina y no filtre secretos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiFlujoTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.security.user.name}")
    private String usuario;

    @Value("${spring.security.user.password}")
    private String clave;

    @Test
    void sinCredencialesResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/materiales"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saludResponde200() throws Exception {
        mockMvc.perform(get("/api/v1/salud").with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("operativo"));
    }

    @Test
    void creaListaYObtieneMaterial() throws Exception {
        mockMvc.perform(post("/api/v1/materiales")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Algodon peinado","tipo":"tela","color":"crudo",
                                 "unidad":"METRO","stock":"120","stockMinimo":"20","activo":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Algodon peinado"))
                .andExpect(jsonPath("$.unidad").value("METRO"));

        // No se compara el total: la base puede tener datos de otros usos.
        mockMvc.perform(get("/api/v1/materiales").with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Algodon peinado")));
    }

    @Test
    void materialSinNombreResponde400ConCampo() throws Exception {
        mockMvc.perform(post("/api/v1/materiales")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"tela","unidad":"METRO","stock":"1","stockMinimo":"0"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").value("el nombre es obligatorio"));
    }

    @Test
    void materialInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/materiales/999999").with(httpBasic(usuario, clave)))
                .andExpect(status().isNotFound());
    }

    /**
     * Flujo completo del módulo de viabilidad: una tela no alcanza, así que la
     * prenda no es viable y se sugiere solo otra tela del mismo color.
     */
    @Test
    void analizaViabilidadConAlternativa() throws Exception {
        int telaInsuficiente = crearMaterial("Viscosa", "tela", "negro", "4");
        int telaSuficiente = crearMaterial("Viscosa premium", "tela", "negro", "80");
        int telaOtroColor = crearMaterial("Algodon peinado", "tela", "crudo", "120");

        int prenda = crearPrenda();

        mockMvc.perform(post("/api/v1/prendas/" + prenda + "/materiales")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materialId":%d,"cantidadRequerida":"10"}
                                """.formatted(telaInsuficiente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suficiente").value(false));

        mockMvc.perform(post("/api/v1/viabilidad/" + prenda).with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viable").value(false))
                .andExpect(jsonPath("$.puntaje").value(0))
                .andExpect(jsonPath("$.detalles", hasSize(1)))
                // La tela negra sirve como sustituta...
                .andExpect(jsonPath("$.alternativas[*].materialId", hasItem(telaSuficiente)))
                // ...pero la de color crudo, no: mismo tipo no basta.
                .andExpect(jsonPath("$.alternativas[*].materialId", not(hasItem(telaOtroColor))));
    }

    /** Con stock suficiente, el veredicto es viable y no hay alternativas. */
    @Test
    void prendaConStockSuficienteEsViable() throws Exception {
        int tela = crearMaterial("Lino", "tela", "crudo", "200");
        int prenda = crearPrenda();

        mockMvc.perform(post("/api/v1/prendas/" + prenda + "/materiales")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materialId":%d,"cantidadRequerida":"15"}
                                """.formatted(tela)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suficiente").value(true));

        mockMvc.perform(post("/api/v1/viabilidad/" + prenda).with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viable").value(true))
                .andExpect(jsonPath("$.puntaje").value(100))
                .andExpect(jsonPath("$.alternativas", hasSize(0)));
    }

    @Test
    void tendenciaConMismaUrlResponde409() throws Exception {
        String cuerpo = """
                {"titulo":"Minimalismo","region":"EUROPA","fuente":"PINTEREST",
                 "urlOrigen":"https://pinterest.com/pin/777"}
                """;

        mockMvc.perform(post("/api/v1/tendencias").with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tendencias").with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void prendaInexistenteResponde404() throws Exception {
        mockMvc.perform(post("/api/v1/viabilidad/999999").with(httpBasic(usuario, clave)))
                .andExpect(status().isNotFound());
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
        return extraerId(respuesta);
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
        return extraerId(respuesta);
    }

    private int extraerId(String json) {
        return Integer.parseInt(json.replaceAll("(?s).*\"id\":\\s*(\\d+).*", "$1").trim());
    }
}
