package com.bulon.stock.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

import static com.bulon.stock.usuarios.Rol.ADMINISTRADOR;
import static com.bulon.stock.usuarios.Rol.ENCARGADO_DEPOSITO;

/**
 * Autenticación HTTP Basic sin sesión: el frontend manda usuario y contraseña en cada request.
 * Matriz de permisos (Entrega 1, 4.4):
 * <ul>
 *   <li>Administrador: todo, incluida la gestión de usuarios.</li>
 *   <li>Encargado de depósito: todo menos usuarios.</li>
 *   <li>Vendedor: consulta todo menos usuarios y registra ventas y devoluciones
 *       ({@code TipoMovimiento.permitidoPara}).</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sin WWW-Authenticate el navegador no muestra su diálogo de login ni cachea credenciales
                .httpBasic(basic -> basic.authenticationEntryPoint((req, res, ex) ->
                        problema(res, HttpServletResponse.SC_UNAUTHORIZED, "Usuario o contraseña incorrectos")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                problema(res, HttpServletResponse.SC_UNAUTHORIZED, "Iniciá sesión para continuar"))
                        .accessDeniedHandler((req, res, ex) ->
                                problema(res, HttpServletResponse.SC_FORBIDDEN, "Tu rol no tiene permiso para esta operación")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/usuarios/**").hasRole(ADMINISTRADOR.name())
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/movimientos").authenticated()
                        .anyRequest().hasAnyRole(ADMINISTRADOR.name(), ENCARGADO_DEPOSITO.name()))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void problema(HttpServletResponse res, int status, String detalle) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"status\":%d,\"detail\":\"%s\"}".formatted(status, detalle));
    }
}
