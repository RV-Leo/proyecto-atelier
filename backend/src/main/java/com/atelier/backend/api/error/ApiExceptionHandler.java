package com.atelier.backend.api.error;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.atelier.backend.ia.IaNoDisponibleException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Convierte las excepciones del backend en respuestas JSON consistentes.
 *
 * <p>Sin esto, un 404 o un JSON mal formado devolveria la pagina de error por
 * defecto de Spring, que el front de Django y la app movil no pueden leer.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Cuerpo de error estandar para todos los fallos de la API. */
    public record ErrorApi(
            OffsetDateTime timestamp,
            int status,
            String error,
            String mensaje,
            Map<String, String> campos) {
    }

    /**
     * Un valor ilegible en la ruta (por ejemplo una región que no existe) no es
     * un conflicto sino un error del cliente; sin esto Spring lo deja caer en
     * un mensaje crudo de Java.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> valorEnRutaInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El valor \"" + ex.getValue() + "\" no es valido para el parametro \""
                + ex.getName() + "\"";
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            mensaje += ". Valores aceptados: "
                    + java.util.Arrays.toString(ex.getRequiredType().getEnumConstants());
        }
        return responder(HttpStatus.BAD_REQUEST, mensaje, null);
    }

    /**
     * El servidor de IA no respondió.
     *
     * <p>503 y no 500: el fallo es del proveedor de modelos, no de la petición.
     * Así quien llama sabe que puede reintentar más tarde y no culpa a su JSON.
     */
    @ExceptionHandler(IaNoDisponibleException.class)
    public ResponseEntity<ErrorApi> iaNoDisponible(IaNoDisponibleException ex) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> enConflicto(IllegalArgumentException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorApi> noEncontrado(EntityNotFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "valor invalido" : fe.getDefaultMessage(),
                        (primero, segundo) -> primero,
                        LinkedHashMap::new));
        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> cuerpoIlegible(HttpMessageNotReadableException ex) {
        String mensaje = "El cuerpo de la peticion no se puede leer: revisa el formato JSON y los tipos de campo";

        // Jackson ya sabe qué valores acepta un enum. Sin esto, un enum mal
        // escrito devuelve un mensaje genérico y quien consume la API no sabe
        // qué puso mal.
        Throwable causa = ex.getCause();
        if (causa != null && causa.getMessage() != null
                && causa.getMessage().contains("from String")) {
            mensaje = causa.getMessage();
        }
        return responder(HttpStatus.BAD_REQUEST, mensaje, null);
    }

    private ResponseEntity<ErrorApi> responder(HttpStatus estado, String mensaje, Map<String, String> campos) {
        ErrorApi cuerpo = new ErrorApi(
                OffsetDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                campos == null || campos.isEmpty() ? null : campos);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
