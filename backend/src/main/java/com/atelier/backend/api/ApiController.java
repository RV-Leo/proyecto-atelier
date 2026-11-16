package com.atelier.backend.api;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de entrada de prueba de la API.
 *
 * <p>Sirve para comprobar que el servicio levanta, que responde JSON y que la
 * seguridad esta activa. Los endpoints de los modulos se agregan despues bajo
 * {@code /api/v1/...}.
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    @GetMapping("/salud")
    public Map<String, Object> salud() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("servicio", "atelier-backend");
        cuerpo.put("estado", "operativo");
        cuerpo.put("timestamp", OffsetDateTime.now());
        return cuerpo;
    }
}
