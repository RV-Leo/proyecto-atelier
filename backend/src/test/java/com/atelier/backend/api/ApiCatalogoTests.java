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
 * Pruebas del catálogo que consume la web de Django (decisión de arquitectura:
 * la web usa esta API en lugar de tener sus propias tablas).
 *
 * <p>Cubren la paginación, los filtros y la búsqueda por slug, que son los tres
 * puntos en los que la web depende de la API para poder funcionar.
 *
 * <p>Los datos se crean con el prefijo "Zoe" y se filtran por ese término, para
 * que las pruebas no dependan de lo que haya en la base de desarrollo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiCatalogoTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.security.user.name}")
    private String usuario;

    @Value("${spring.security.user.password}")
    private String clave;

    // --- Paginación -------------------------------------------------------

    /**
     * La web pagina el catálogo a 12 por página y necesita el total para pintar
     * el número de páginas, así que el envelope tiene que devolverlo.
     */
    @Test
    void paginacionDevuelveLaPaginaYElTotal() throws Exception {
        crearMaterial("Zoe Pagina Alfa", "0");
        crearMaterial("Zoe Pagina Beta", "0");
        crearMaterial("Zoe Pagina Gamma", "0");

        mockMvc.perform(get("/api/v1/materiales")
                        .param("q", "Zoe Pagina")
                        .param("pagina", "1")
                        .param("tamano", "2")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(2))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.primera").value(true))
                .andExpect(jsonPath("$.ultima").value(false));

        mockMvc.perform(get("/api/v1/materiales")
                        .param("q", "Zoe Pagina")
                        .param("pagina", "2")
                        .param("tamano", "2")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.primera").value(false))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    /**
     * Sin parámetros de paginación se devuelve todo en una sola página: el
     * inventario y el panel de la web cargan el listado completo y no deben
     * romperse por este cambio.
     */
    @Test
    void sinParametrosDePaginaSeDevuelveTodo() throws Exception {
        crearMaterial("Zoe Completo Uno", "0");
        crearMaterial("Zoe Completo Dos", "0");

        mockMvc.perform(get("/api/v1/materiales")
                        .param("q", "Zoe Completo")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primera").value(true))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    /** Una página pedida fuera de rango responde vacía, no es un error. */
    @Test
    void paginaFueraDeRangoRespondeVacia() throws Exception {
        crearMaterial("Zoe Rango Unico", "0");

        mockMvc.perform(get("/api/v1/materiales")
                        .param("q", "Zoe Rango")
                        .param("pagina", "50")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(1));
    }

    // --- Estado del material ----------------------------------------------

    /**
     * La web ya clasificaba el stock como agotado / bajo / disponible. El
     * cálculo pasa a vivir en la API para que web y móvil vean lo mismo.
     */
    @Test
    void estadoSeCalculaSegunElStockYElMinimo() throws Exception {
        crearMaterial("Zoe Estado Agotado", "0");
        crearMaterial("Zoe Estado Bajo", "4");
        crearMaterial("Zoe Estado Disponible", "100");

        mockMvc.perform(get("/api/v1/materiales")
                        .param("q", "Zoe Estado")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.nombre == 'Zoe Estado Agotado')].estado",
                        hasItem("AGOTADO")))
                .andExpect(jsonPath("$.items[?(@.nombre == 'Zoe Estado Bajo')].estado",
                        hasItem("BAJO")))
                .andExpect(jsonPath("$.items[?(@.nombre == 'Zoe Estado Disponible')].estado",
                        hasItem("DISPONIBLE")));
    }

    /** El filtro por estado debe devolver exactamente los de ese estado. */
    @Test
    void filtroPorEstadoFiltraElListado() throws Exception {
        crearMaterial("Zoe Filtro Agotado", "0");
        crearMaterial("Zoe Filtro Disponible", "999");

        mockMvc.perform(get("/api/v1/materiales")
                        .param("q", "Zoe Filtro")
                        .param("estado", "AGOTADO")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].nombre").value("Zoe Filtro Agotado"))
                .andExpect(jsonPath("$.items[0].estado").value("AGOTADO"));
    }

    // --- Búsqueda libre ---------------------------------------------------

    @Test
    void busquedaLibreFiltraPorNombre() throws Exception {
        crearMaterial("Zoe Busqueda Singular", "0");
        crearMaterial("Zoe Otra Cosa", "0");

        mockMvc.perform(get("/api/v1/materiales")
                        .param("q", "Singular")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].nombre", hasItem("Zoe Busqueda Singular")))
                .andExpect(jsonPath("$.items[*].nombre", not(hasItem("Zoe Otra Cosa"))));
    }

    // --- Slug -------------------------------------------------------------

    /** La web abre el detalle con /catalogo/<slug>/, no con el id. */
    @Test
    void prendaSeEncuentraPorSlug() throws Exception {
        mockMvc.perform(post("/api/v1/prendas")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Vestido de verano Zoe","tipoPrenda":"vestido",
                                 "estilo":"casual","colores":"negro","tela":"viscosa"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("vestido-de-verano-zoe"));

        mockMvc.perform(get("/api/v1/prendas/slug/vestido-de-verano-zoe")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Vestido de verano Zoe"));
    }

    /** Dos prendas pueden llamarse igual: el segundo slug lleva sufijo. */
    @Test
    void dosPrendasIgualesGeneranSlugsDistintos() throws Exception {
        String nombre = """
                {"nombre":"Blusa Zoe","tipoPrenda":"blusa","estilo":"casual"}
                """;

        mockMvc.perform(post("/api/v1/prendas").with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON).content(nombre))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("blusa-zoe"));

        mockMvc.perform(post("/api/v1/prendas").with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON).content(nombre))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("blusa-zoe-2"));
    }

    /** Un slug inexistente responde 404, no una página en blanco. */
    @Test
    void slugInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/prendas/slug/no-existe-zoe")
                        .with(httpBasic(usuario, clave)))
                .andExpect(status().isNotFound());
    }

    // --- Ayudantes --------------------------------------------------------

    private void crearMaterial(String nombre, String stock) throws Exception {
        mockMvc.perform(post("/api/v1/materiales")
                        .with(httpBasic(usuario, clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","tipo":"tela","color":"crudo",
                                 "unidad":"METRO","stock":"%s","stockMinimo":"15","activo":true}
                                """.formatted(nombre, stock)))
                .andExpect(status().isCreated());
    }
}
