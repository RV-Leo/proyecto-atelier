package com.atelier.backend.servicio;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atelier.backend.api.dto.PromptResponse;
import com.atelier.backend.api.dto.PropuestaDisenoResponse;
import com.atelier.backend.dominio.EstadoPrenda;
import com.atelier.backend.dominio.Prenda;
import com.atelier.backend.dominio.PropuestaDiseno;
import com.atelier.backend.ia.ClienteIa;
import com.atelier.backend.repositorio.PrendaRepository;
import com.atelier.backend.repositorio.PropuestaDisenoRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Diseño generativo: de la descripción del usuario a las propuestas de imagen.
 *
 * <p>Sigue el flujo de {@code docs/modelos-ia.md}:
 * <ol>
 *   <li>el usuario describe la prenda (ya guardada en {@link Prenda});</li>
 *   <li>se construye el prompt con el modelo de texto;</li>
 *   <li>Qwen-Image genera varias propuestas, que se guardan una por fila.</li>
 * </ol>
 *
 * <p>El prompt se guarda en la prenda y en cada propuesta: es lo que permite
 * reproducir una imagen o refinarla después sin empezar de cero.
 */
@Service
@Transactional
public class DisenoServicio {

    private static final Logger log = LoggerFactory.getLogger(DisenoServicio.class);

    private final PrendaRepository prendas;
    private final PropuestaDisenoRepository propuestas;
    private final ClienteIa clienteIa;

    public DisenoServicio(PrendaRepository prendas, PropuestaDisenoRepository propuestas, ClienteIa clienteIa) {
        this.prendas = prendas;
        this.propuestas = propuestas;
        this.clienteIa = clienteIa;
    }

    /**
     * Devuelve el prompt listo para el modelo de imágenes, sin generar nada.
     *
     * <p>Útil para que el diseñador corrija la instrucción antes de gastar
     * tiempo de GPU.
     */
    @Transactional(readOnly = true)
    public PromptResponse construirPrompt(Long prendaId) {
        Prenda prenda = obtener(prendaId);
        String prompt = clienteIa.construirPromptDeImagen(prenda);
        String fuente = clienteIa.disponible()
                ? PromptResponse.FUENTE_MODELO
                : PromptResponse.FUENTE_PLANTILLA;
        return new PromptResponse(prompt, fuente);
    }

    /**
     * Genera propuestas de imagen y las guarda.
     *
     * <p>La prenda pasa a {@link EstadoPrenda#GENERANDO} mientras el modelo
     * trabaja y a {@link EstadoPrenda#GENERADA} cuando las propuestas quedan
     * registradas. Si el modelo falla, la transacción se revierte y la prenda
     * conserva su estado anterior.
     */
    public List<PropuestaDisenoResponse> generarPropuestas(Long prendaId, int cantidad) {
        Prenda prenda = obtener(prendaId);

        String prompt = clienteIa.construirPromptDeImagen(prenda);
        prenda.setPromptIa(prompt);
        prenda.setEstado(EstadoPrenda.GENERANDO);
        prenda = prendas.save(prenda);

        List<ClienteIa.ImagenGenerada> generadas = clienteIa.generarImagenes(prompt, cantidad);

        List<PropuestaDisenoResponse> respuesta = new ArrayList<>();
        for (ClienteIa.ImagenGenerada imagen : generadas) {
            PropuestaDiseno propuesta = new PropuestaDiseno();
            propuesta.setPrenda(prenda);
            propuesta.setImagenUrl(imagen.url());
            propuesta.setPromptUsado(imagen.promptUsado());
            propuesta.setSeleccionada(false);
            respuesta.add(PropuestaDisenoResponse.desde(propuestas.save(propuesta)));
        }

        prenda.setEstado(EstadoPrenda.GENERADA);
        prendas.save(prenda);

        log.info("Generadas {} propuestas para la prenda {}", respuesta.size(), prendaId);
        return respuesta;
    }

    @Transactional(readOnly = true)
    public List<PropuestaDisenoResponse> listarPropuestas(Long prendaId) {
        if (!prendas.existsById(prendaId)) {
            throw new EntityNotFoundException("No existe la prenda " + prendaId);
        }
        return propuestas.findByPrendaId(prendaId).stream()
                .map(PropuestaDisenoResponse::desde)
                .toList();
    }

    /**
     * Marca una propuesta como la elegida y deselecciona las demás.
     *
     * <p>La tabla no impone unicidad de la selección, así que la garantiza el
     * servicio: solo una propuesta puede estar elegida por prenda.
     */
    public PropuestaDisenoResponse seleccionar(Long propuestaId) {
        PropuestaDiseno elegida = propuestas.findById(propuestaId)
                .orElseThrow(() -> new EntityNotFoundException("No existe la propuesta " + propuestaId));

        // La entidad esta gestionada por la transaccion: el bucle tambien la
        // alcanza, asi que no hace falta volver a guardarla aparte.
        Long prendaId = elegida.getPrenda().getId();
        for (PropuestaDiseno otra : propuestas.findByPrendaId(prendaId)) {
            boolean debeQuedarMarcada = otra.getId().equals(propuestaId);
            if (otra.isSeleccionada() != debeQuedarMarcada) {
                otra.setSeleccionada(debeQuedarMarcada);
                propuestas.save(otra);
            }
        }

        return PropuestaDisenoResponse.desde(elegida);
    }

    private Prenda obtener(Long id) {
        return prendas.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la prenda " + id));
    }
}
