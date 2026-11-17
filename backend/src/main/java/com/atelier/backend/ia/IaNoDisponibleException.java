package com.atelier.backend.ia;

/** Error al llamar a un proveedor de IA no disponible. */
public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public IaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
