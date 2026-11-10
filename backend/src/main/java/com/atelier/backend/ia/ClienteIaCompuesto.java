package com.atelier.backend.ia;

import java.util.List;

import com.atelier.backend.dominio.Prenda;

/**
 * Cliente de IA formado por dos fuentes: texto de una, imágenes de otra.
 *
 * <p>Permite combinar proveedores sin mezclar sus protocolos. Por ejemplo,
 * Gemini (compatible con OpenAI) redactando el prompt y la viabilidad, y
 * Pollinations generando las imágenes, que usa un endpoint propio y no entiende
 * de {@code /chat/completions}.
 *
 * <p>{@link #disponible()} refleja solo la fuente de <em>texto</em>, porque es
 * lo que decide si el prompt sale del modelo o de la plantilla. Las imágenes
 * tienen su propio mecanismo de fallo: si fallan, se propaga
 * {@link IaNoDisponibleException} y nada más.
 */
public class ClienteIaCompuesto implements ClienteIa {

    private final ClienteIa texto;
    private final GeneradorImagenes imagenes;

    public ClienteIaCompuesto(ClienteIa texto, GeneradorImagenes imagenes) {
        this.texto = texto;
        this.imagenes = imagenes;
    }

    @Override
    public boolean disponible() {
        return texto.disponible();
    }

    @Override
    public String construirPromptDeImagen(Prenda prenda) {
        return texto.construirPromptDeImagen(prenda);
    }

    @Override
    public String explicarViabilidad(ContextoViabilidad contexto) {
        return texto.explicarViabilidad(contexto);
    }

    @Override
    public List<ImagenGenerada> generarImagenes(String prompt, int cantidad) {
        return imagenes.generar(prompt, cantidad);
    }
}
