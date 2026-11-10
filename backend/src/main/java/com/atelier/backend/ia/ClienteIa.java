package com.atelier.backend.ia;

import java.util.List;

import com.atelier.backend.api.dto.ViabilidadResponse;
import com.atelier.backend.dominio.Prenda;

/**
 * Puente hacia los modelos de IA.
 *
 * <p>El README lo define así: "API REST (Spring Boot) ──> Gateway IA ──>
 * Modelos Qwen". Esta interfaz es esa flecha. El backend nunca habla con el
 * modelo directamente, sino a través de este contrato, de modo que se puede
 * cambiar de servidor de inferencia (o apuntar al gateway de Django) sin
 * tocar la lógica de negocio.
 *
 * <p>Todas las operaciones deben degradar con sensatez si el modelo no
 * responde: el backend tiene que seguir pudiendo calcular stock y viabilidad.
 */
public interface ClienteIa {

    /**
     * Si hay servidor de IA configurado.
     *
     * <p>Cuando es {@code false} el backend sigue siendo usable: los servicios
     * usan la vía determinista y solo se privan del texto redactado por el
     * modelo.
     */
    boolean disponible();

    /**
     * Convierte la descripción del usuario en una instrucción lista para el
     * modelo de imágenes (paso 2 del flujo de {@code docs/modelos-ia.md}).
     */
    String construirPromptDeImagen(Prenda prenda);

    /**
     * Redacta la explicación de un análisis de viabilidad (paso 5 del flujo).
     *
     * @param contexto el resultado ya calculado por Spring Boot; el modelo solo
     *                 lo narra, no decide
     * @return la explicación, o {@code null} si el modelo no devolvió nada
     *         útil y conviene usar la plantilla determinista
     */
    String explicarViabilidad(ContextoViabilidad contexto);

    /**
     * Genera imágenes de la prenda (paso 3 del flujo, Qwen-Image).
     *
     * @param prompt instrucción para el modelo de imágenes
     * @param cantidad cuántas propuestas pedir
     * @return una por imagen generada
     */
    List<ImagenGenerada> generarImagenes(String prompt, int cantidad);

    /** Datos necesarios para que el modelo explique un veredicto. */
    record ContextoViabilidad(
            Prenda prenda,
            boolean viable,
            int puntaje,
            List<ViabilidadResponse.Detalle> detalles,
            List<ViabilidadResponse.Alternativa> alternativas) {
    }

    /**
     * Una imagen devuelta por el modelo.
     *
     * @param url dónde queda la imagen: una URL si el modelo la devolvió
     *            remota, o la ruta local si venía en base64 y hubo que guardarla
     * @param promptUsado la instrucción que produjo la imagen, para poder
     *                    reproducirla o refinarla
     */
    record ImagenGenerada(String url, String promptUsado) {
    }
}
