package com.atelier.backend.ia.agentes;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.atelier.backend.ia.ClienteIa.MensajeChat;

/** Registro y selección de especialistas para el chat de ATELIER. */
@Component
public class AgentesAtelier {

    private static final String REGLA_DE_DATOS = "Usa solo los datos del contexto de ATELIER. "
            + "Si falta un dato real, admítelo; no lo deduzcas ni lo inventes.";
    private static final AgenteAtelier CORTESIA = new AgenteAtelier(
            "bienvenida ATELIER",
            "Saluda con brevedad y ofrece ayuda únicamente con ATELIER y el diseño textil.",
            List.of());

    private final List<AgenteAtelier> agentes = List.of(
            new AgenteAtelier("inventario y viabilidad",
                    "Atiende consultas de stock, materiales, ficha técnica y producción. " + REGLA_DE_DATOS,
                    List.of("inventario", "stock", "existencias", "viabilidad", "producir",
                            "produccion", "ficha tecnica", "material requerido", "cuanto material")),
            new AgenteAtelier("tendencias",
                    "Atiende tendencias, temporadas, colecciones y paletas textiles de ATELIER. " + REGLA_DE_DATOS,
                    List.of("tendencia", "tendencias", "coleccion", "colecciones", "temporada", "paleta")),
            new AgenteAtelier("catálogo",
                    "Atiende consultas de productos y telas que ofrece el catálogo. " + REGLA_DE_DATOS,
                    List.of("catalogo", "producto", "productos", "precio", "comprar", "venden",
                            "disponible", "disponibilidad", "agotado")),
            new AgenteAtelier("diseño textil",
                    "Ayuda con prendas, estilos, telas, colores, confección y propuestas de diseño. "
                            + "Distingue sugerencias creativas de datos reales del catálogo.",
                    List.of("diseno", "disena", "disenar", "disene", "disenes", "prenda", "vestido",
                            "blusa", "camisa", "camiseta", "falda", "chaqueta", "pantalon", "abrigo",
                            "ropa", "moda", "tela", "textil", "algodon", "lino", "viscosa", "denim",
                            "costura", "patron", "color", "colores", "talla", "fibra", "tejido",
                            "estilo", "elegante", "minimalista", "brillante", "propuesta", "imagen")),
            new AgenteAtelier("proyecto ATELIER",
                    "Explica solo el funcionamiento y la arquitectura documentada de ATELIER. "
                            + "Si preguntan por un detalle que no está en el contexto, indícalo.",
                    List.of("atelier", "proyecto", "pagina", "web", "plataforma", "aplicacion", "sistema",
                            "base de datos", "base de dato", "postgresql", "flyway", "django", "spring",
                            "backend", "frontend", "android", "kotlin", "api", "gemini", "pollinations",
                            "chat", "funcionalidad", "funciona", "estructura", "arquitectura", "cuenta",
                            "login", "iniciar sesion", "error", "repositorio", "modelo", "tabla")));

    /** Clasifica el mensaje actual y, solo si parece un seguimiento breve, usa el historial. */
    public Optional<AgenteAtelier> seleccionar(List<MensajeChat> mensajes) {
        if (mensajes == null || mensajes.isEmpty()) {
            return Optional.empty();
        }
        int ultimoUsuario = -1;
        for (int i = mensajes.size() - 1; i >= 0; i--) {
            if (mensajes.get(i) != null && "user".equalsIgnoreCase(mensajes.get(i).role())) {
                ultimoUsuario = i;
                break;
            }
        }
        if (ultimoUsuario < 0) {
            return Optional.empty();
        }

        String texto = AgenteAtelier.normalizar(mensajes.get(ultimoUsuario).content());
        Optional<AgenteAtelier> agente = buscar(texto);
        if (mencionaTemaAjeno(texto)) {
            return Optional.empty();
        }
        if (agente.isPresent()) {
            return agente;
        }
        if (esCortesia(texto)) {
            return Optional.of(CORTESIA);
        }
        if (!esSeguimientoBreve(texto)) {
            return Optional.empty();
        }
        for (int i = ultimoUsuario - 1; i >= 0; i--) {
            MensajeChat anterior = mensajes.get(i);
            if (anterior == null) {
                continue;
            }
            Optional<AgenteAtelier> previo = buscar(AgenteAtelier.normalizar(anterior.content()));
            if (previo.isPresent()) {
                return previo;
            }
        }
        return Optional.empty();
    }

    public String instrucciones(AgenteAtelier agente) {
        return "Especialista activo: " + agente.nombre() + ". " + agente.instrucciones();
    }

    public String respuestaFueraDeDominio() {
        return "Soy Atelier IA y solo puedo ayudar con ATELIER: prendas, telas, catálogo, "
                + "tendencias, inventario, producción o el funcionamiento del proyecto. "
                + "¿Qué te gustaría consultar?";
    }

    private Optional<AgenteAtelier> buscar(String texto) {
        return agentes.stream().filter(agente -> agente.reconoce(texto)).findFirst();
    }

    private boolean esCortesia(String texto) {
        return List.of("hola", "buenos dias", "buenas tardes", "buenas noches", "gracias",
                "muchas gracias", "ok", "okay", "listo", "entendido", "adios", "hasta luego")
                .contains(texto);
    }

    private boolean mencionaTemaAjeno(String texto) {
        return List.of("historia de la torre", "torre de babel", "historia biblica", "biblia",
                "capital de", "presidente", "politica", "partido politico", "religion", "futbol",
                "videojuego", "receta", "tarea escolar", "tarea de matematica", "pronostico del clima")
                .stream().anyMatch(texto::contains);
    }

    private boolean esSeguimientoBreve(String texto) {
        if (texto.split(" ").length > 6) {
            return false;
        }
        return List.of("mas", "menos", "otra", "otro", "esa", "ese", "eso", "esta", "este",
                "cambialo", "cambiala", "modificalo", "modificala", "hazlo", "hazla", "asi",
                "me gusta", "cual recomiendas", "que opinas", "puedes ajustarlo", "puedes cambiarlo")
                .stream().anyMatch(texto::contains);
    }
}
