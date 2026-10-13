package com.atelier.backend.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atelier.backend.api.dto.PrendaMaterialResponse;
import com.atelier.backend.api.dto.ViabilidadResponse;
import com.atelier.backend.dominio.AlternativaStock;
import com.atelier.backend.dominio.AnalisisViabilidad;
import com.atelier.backend.dominio.Material;
import com.atelier.backend.dominio.Prenda;
import com.atelier.backend.dominio.PrendaMaterial;
import com.atelier.backend.ia.ClienteIa;
import com.atelier.backend.repositorio.AlternativaStockRepository;
import com.atelier.backend.repositorio.AnalisisViabilidadRepository;
import com.atelier.backend.repositorio.MaterialRepository;
import com.atelier.backend.repositorio.PrendaMaterialRepository;
import com.atelier.backend.repositorio.PrendaRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Comparación de una idea de prenda contra el stock real.
 *
 * <p>Implementa el paso 4 y 5 del flujo descrito en {@code docs/modelos-ia.md}:
 * "Spring Boot revisa si hay stock" y "explica si se puede producir la prenda
 * y qué alternativas hay".
 *
 * <p>La regla es deliberadamente simple y determinista para esta primera
 * versión: la prenda es viable si cada material de su ficha técnica tiene
 * stock igual o mayor a lo requerido. El puntaje es el porcentaje de
 * materiales cubiertos. La redacción del comentario queda lista para que la
 * consume un modelo de IA más adelante.
 */
@Service
@Transactional
public class ViabilidadServicio {

    /** Cuántas alternativas sugerir como máximo por cada material faltante. */
    private static final int MAX_ALTERNATIVAS = 3;

    private final PrendaRepository prendas;
    private final PrendaMaterialRepository fichaTecnica;
    private final MaterialRepository materiales;
    private final AnalisisViabilidadRepository analisis;
    private final AlternativaStockRepository alternativas;
    private final ClienteIa clienteIa;

    public ViabilidadServicio(
            PrendaRepository prendas,
            PrendaMaterialRepository fichaTecnica,
            MaterialRepository materiales,
            AnalisisViabilidadRepository analisis,
            AlternativaStockRepository alternativas,
            ClienteIa clienteIa) {
        this.prendas = prendas;
        this.fichaTecnica = fichaTecnica;
        this.materiales = materiales;
        this.analisis = analisis;
        this.alternativas = alternativas;
        this.clienteIa = clienteIa;
    }

    /** Devuelve la ficha técnica de una prenda con el estado de cada material. */
    @Transactional(readOnly = true)
    public List<PrendaMaterialResponse> fichaTecnica(Long prendaId) {
        if (!prendas.existsById(prendaId)) {
            throw new EntityNotFoundException("No existe la prenda " + prendaId);
        }
        return fichaTecnica.findByPrendaId(prendaId).stream()
                .map(this::aDetalle)
                .toList();
    }

    /**
     * Analiza la prenda contra el stock y guarda el resultado.
     *
     * @return el análisis completo, con alternativas para lo que falte
     */
    public ViabilidadResponse analizar(Long prendaId) {
        Prenda prenda = prendas.findById(prendaId)
                .orElseThrow(() -> new EntityNotFoundException("No existe la prenda " + prendaId));

        List<PrendaMaterial> requeridos = fichaTecnica.findByPrendaId(prendaId);

        List<ViabilidadResponse.Detalle> detalles = new ArrayList<>();
        List<ViabilidadResponse.Alternativa> alternativasVista = new ArrayList<>();
        List<AlternativaStock> alternativasGuardadas = new ArrayList<>();

        int cubiertos = 0;

        for (PrendaMaterial requerimiento : requeridos) {
            Material material = requerimiento.getMaterial();
            BigDecimal requerido = requerimiento.getCantidadRequerida();
            BigDecimal disponible = material.getStock();
            boolean suficiente = disponible.compareTo(requerido) >= 0;

            if (suficiente) {
                cubiertos++;
            } else {
                guardarAlternativas(material, requerido, alternativasVista, alternativasGuardadas);
            }

            detalles.add(new ViabilidadResponse.Detalle(
                    material.getId(),
                    material.getNombre(),
                    material.getUnidad(),
                    requerido,
                    disponible,
                    suficiente));
        }

        int total = requeridos.size();
        int puntaje = total == 0
                ? 0
                : BigDecimal.valueOf(cubiertos * 100L)
                        .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                        .intValue();
        boolean viable = total > 0 && cubiertos == total;

        // Spring Boot decide; el modelo solo narra. Si no hay servidor de IA o
        // no contesta, se conserva el comentario de plantilla.
        String comentario = redactar(prenda, viable, puntaje, total - cubiertos);
        if (clienteIa.disponible()) {
            String narrado = clienteIa.explicarViabilidad(new ClienteIa.ContextoViabilidad(
                    prenda, viable, puntaje, detalles, alternativasVista));
            if (narrado != null && !narrado.isBlank()) {
                comentario = narrado;
            }
        }

        AnalisisViabilidad registro = new AnalisisViabilidad();
        registro.setPrenda(prenda);
        registro.setViable(viable);
        registro.setPuntaje(puntaje);
        registro.setComentario(comentario);
        AnalisisViabilidad guardado = analisis.save(registro);

        for (AlternativaStock alternativa : alternativasGuardadas) {
            alternativa.setAnalisis(guardado);
            alternativas.save(alternativa);
        }

        return new ViabilidadResponse(
                guardado.getId(),
                prenda.getId(),
                prenda.getNombre(),
                viable,
                puntaje,
                guardado.getComentario(),
                detalles,
                alternativasVista);
    }

    /**
     * Busca materiales activos del mismo tipo y color que sí tengan stock, para
     * sugerirlos como sustituto del que falta.
     *
     * <p>Se exige coincidir también el color a propósito: solo comparar por
     * {@code tipo} sugería sustituir "Viscosa negra" por "Algodón crudo", que
     * aunque ambos son tela no son intercambiables para el mismo diseño.
     */
    private void guardarAlternativas(
            Material faltante,
            BigDecimal requerido,
            List<ViabilidadResponse.Alternativa> vista,
            List<AlternativaStock> guardado) {

        BigDecimal falta = requerido.subtract(faltante.getStock());
        String motivo = "stock insuficiente: faltan " + falta.stripTrailingZeros().toPlainString();

        List<Material> candidatos = materiales.findByActivoTrue().stream()
                .filter(c -> !c.getId().equals(faltante.getId()))
                .filter(c -> c.getTipo().equalsIgnoreCase(faltante.getTipo()))
                .filter(c -> mismoColor(c, faltante))
                .filter(c -> c.getStock().compareTo(falta) >= 0)
                .sorted(Comparator.comparing(Material::getStock).reversed())
                .limit(MAX_ALTERNATIVAS)
                .toList();

        for (Material candidato : candidatos) {
            vista.add(new ViabilidadResponse.Alternativa(
                    candidato.getId(),
                    candidato.getNombre(),
                    candidato.getUnidad(),
                    candidato.getStock(),
                    "sustituye a " + faltante.getNombre() + " (" + motivo + ")"));

            AlternativaStock entidad = new AlternativaStock();
            entidad.setMaterial(faltante);
            entidad.setMaterialAlternativo(candidato);
            entidad.setMotivo(motivo);
            entidad.setCantidadSugerida(falta);
            guardado.add(entidad);
        }
    }

    private PrendaMaterialResponse aDetalle(PrendaMaterial fila) {
        Material material = fila.getMaterial();
        BigDecimal requerido = fila.getCantidadRequerida();
        return new PrendaMaterialResponse(
                fila.getId(),
                material.getId(),
                material.getNombre(),
                material.getUnidad(),
                requerido,
                material.getStock(),
                material.getStock().compareTo(requerido) >= 0);
    }

    /**
     * Dos materiales cuentan como del mismo color si no tienen color definido
     * o si coinciden. Un material sin color es comodín: sirve para varios
     * diseños.
     */
    private boolean mismoColor(Material a, Material b) {
        if (a.getColor() == null || a.getColor().isBlank()
                || b.getColor() == null || b.getColor().isBlank()) {
            return true;
        }
        return a.getColor().equalsIgnoreCase(b.getColor());
    }

    /** Comentario breve que hoy es una plantilla y mañana lo redacta la IA. */
    private String redactar(Prenda prenda, boolean viable, int puntaje, int faltantes) {
        if (viable) {
            return "Se puede producir " + prenda.getNombre()
                    + ": todos los materiales necesarios estan en stock.";
        }
        if (faltantes == 0) {
            return "La prenda no tiene materiales en su ficha tecnica; "
                    + "agrega materiales antes de evaluar la viabilidad.";
        }
        return "No se puede producir " + prenda.getNombre() + " aun: faltan "
                + faltantes + (faltantes == 1 ? " material" : " materiales")
                + ". Cobertura actual del " + puntaje + "%. Revisa las alternativas sugeridas.";
    }
}
