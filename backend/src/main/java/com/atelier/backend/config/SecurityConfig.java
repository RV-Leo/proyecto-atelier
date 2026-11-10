package com.atelier.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API.
 *
 * <p>Todas las rutas exigen autenticacion HTTP Basic (credenciales en
 * {@code spring.security.user.*}). Salvo las de salud, que deben quedar
 * abiertas para que el balanceador o el monitor las consulten sin credenciales.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // La API no usa sesiones de navegador: cada peticion se autentica sola.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Las imágenes generadas son recursos estáticos: un
                        // <img> no puede mandar cabeceras Basic Auth, así que
                        // si esto quedaba protegido los diseños no se veían.
                        // El endpoint que las genera sí sigue protegido.
                        .requestMatchers("/imagenes/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
