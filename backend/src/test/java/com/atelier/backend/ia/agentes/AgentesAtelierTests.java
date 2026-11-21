package com.atelier.backend.ia.agentes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.atelier.backend.api.IaController;
import com.atelier.backend.ia.ClienteIa.MensajeChat;
import com.atelier.backend.ia.ClienteIaNoDisponible;

class AgentesAtelierTests {

    private final AgentesAtelier agentes = new AgentesAtelier();

    @Test
    void rechazaUnaConsultaAjenaAunqueElChatTengaHistorialDeDiseno() {
        var mensajes = List.of(
                new MensajeChat("user", "Diseña un vestido minimalista"),
                new MensajeChat("assistant", "Puedo ayudarte con ese diseño."),
                new MensajeChat("user", "Cuéntame la historia de la Torre de Babel"));

        assertTrue(agentes.seleccionar(mensajes).isEmpty());
    }

    @Test
    void noPermiteQueLaPalabraChatHabiliteUnaConsultaAjena() {
        var seleccionado = agentes.seleccionar(List.of(
                new MensajeChat("user", "Chat, cuéntame la historia de la Torre de Babel")));

        assertTrue(seleccionado.isEmpty());
    }

    @Test
    void enrutaPreguntasDeInventarioAlEspecialistaCorrecto() {
        var seleccionado = agentes.seleccionar(List.of(
                new MensajeChat("user", "¿Cuánto stock de lino hay en inventario?")));

        assertTrue(seleccionado.isPresent());
        assertEquals("inventario y viabilidad", seleccionado.get().nombre());
    }

    @Test
    void reconoceUnaSolicitudDeDisenoDeBlusa() {
        var seleccionado = agentes.seleccionar(List.of(
                new MensajeChat("user", "Diseña una blusa para primavera")));

        assertTrue(seleccionado.isPresent());
        assertEquals("diseño textil", seleccionado.get().nombre());
    }

    @Test
    void conservaElEspecialistaParaUnSeguimientoBreve() {
        var seleccionado = agentes.seleccionar(List.of(
                new MensajeChat("user", "Diseña un vestido de lino"),
                new MensajeChat("assistant", "Te propongo un vestido recto."),
                new MensajeChat("user", "Otra opción")));

        assertTrue(seleccionado.isPresent());
        assertEquals("diseño textil", seleccionado.get().nombre());
    }

    @Test
    void admiteConsultasSobreLaBaseDeDatosDelProyecto() {
        var seleccionado = agentes.seleccionar(List.of(
                new MensajeChat("user", "¿Cómo se organiza la base de datos del proyecto?")));

        assertTrue(seleccionado.isPresent());
        assertEquals("proyecto ATELIER", seleccionado.get().nombre());
    }

    @Test
    void cargaElSkillConLimitesDeDominio() {
        var skill = new AtelierSkill();

        assertTrue(skill.instrucciones().contains("Rechaza con amabilidad preguntas ajenas"));
        assertFalse(skill.instrucciones().isBlank());
    }

    @Test
    void respondeFueraDeDominioSinLlamarAlProveedorDeIa() {
        var orquestador = new OrquestadorAgentes(
                new ClienteIaNoDisponible(), new AtelierSkill(), agentes);

        String respuesta = orquestador.conversar(List.of(
                new MensajeChat("user", "Cuéntame la historia de la Torre de Babel")), null);

        assertTrue(respuesta.contains("solo puedo ayudar con ATELIER"));
    }

    @Test
    void endpointDeChatRechazaLaConsultaAjenaSinNecesitarGemini() throws Exception {
        var orquestador = new OrquestadorAgentes(
                new ClienteIaNoDisponible(), new AtelierSkill(), agentes);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new IaController(new ClienteIaNoDisponible(), orquestador)).build();

        mvc.perform(post("/api/v1/ia/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages":[{"role":"user","content":"Cuéntame la historia de la Torre de Babel"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value(
                        org.hamcrest.Matchers.containsString("solo puedo ayudar con ATELIER")));
    }
}
