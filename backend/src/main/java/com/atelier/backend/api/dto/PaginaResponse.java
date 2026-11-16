package com.atelier.backend.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Página de resultados.
 *
 * <p>Los nombres de los campos se eligieron para calcar el paginador de Django
 * ({@code Paginator.count}, {@code num_pages}, {@code page.number}), porque el
 * consumidor principal de esta API es la web. Así la vista no tiene que traducir
 * nada.
 *
 * <p>La paginación es <strong>opcional a propósito</strong>: si no se mandan los
 * parámetros {@code pagina} ni {@code tamano}, se devuelve todo en una sola
 * página. Así el catálogo puede paginar a 12 por página mientras el inventario
 * y los informes siguen recibiendo la lista completa, como ya lo hacen hoy.
 */
public record PaginaResponse<T>(
        List<T> items,
        int pagina,
        int tamano,
        long total,
        int totalPaginas,
        boolean primera,
        boolean ultima) {

    /** Tamaño por defecto y tope máximo, para que nadie pueda pedir la tabla entera. */
    private static final int TAMANO_DEFECTO = 20;
    private static final int TAMANO_MAXIMO = 100;

    public static <T> PaginaResponse<T> desde(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    /** Todo el resultado en una sola página, para los que no paginan. */
    public static <T> PaginaResponse<T> completa(List<T> items) {
        return new PaginaResponse<>(items, 1, items.size(), items.size(), 1, true, true);
    }

    /**
     * Traduce los parámetros de la URL a un {@link Pageable}.
     *
     * <p>{@code pagina} es base 1 (como en Django), no base 0: pedir
     * {@code ?pagina=1} devuelve la primera página.
     */
    public static Pageable aPageable(Integer pagina, Integer tamano) {
        return aPageable(pagina, tamano, Sort.unsorted());
    }

    /** Igual que {@link #aPageable(Integer, Integer)} pero con orden fijo. */
    public static Pageable aPageable(Integer pagina, Integer tamano, Sort orden) {
        int numero = pagina == null ? 1 : Math.max(pagina, 1);
        int tamanio = (tamano == null || tamano < 1)
                ? TAMANO_DEFECTO
                : Math.min(tamano, TAMANO_MAXIMO);
        return PageRequest.of(numero - 1, tamanio, orden);
    }
}
