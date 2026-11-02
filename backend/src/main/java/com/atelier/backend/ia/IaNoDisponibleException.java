package com.atelier.backend.ia;

/**
 * El servidor de IA no está disponible.
 *
 * <p>Se traduce a HTTP 503 para que quien llama sepa que el fallo es del
 * proveedor de IA y no de su petición, y pueda reintentar más tarde.
 */
public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public IaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
