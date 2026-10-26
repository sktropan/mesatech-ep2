package cl.duoc.mesatech.catalogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Solo para la demo del flujo RabbitMQ (SPRING_PROFILES_ACTIVE=demo).
 * Abre únicamente los endpoints del flujo; todo lo demás queda denegado.
 * Sin este perfil se usa SecurityConfig con JWT de Entra ID.
 */
@Configuration
@Profile("demo")
public class DemoSecurityConfig {

    @Bean
    SecurityFilterChain demoSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/v1/catalogo/categorias").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/catalogo").permitAll()
                .requestMatchers(HttpMethod.GET, "/error").permitAll()
                .anyRequest().denyAll()
            );
        return http.build();
    }
}
