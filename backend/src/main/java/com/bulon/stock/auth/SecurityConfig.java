package com.bulon.stock.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Seguridad de la API.
 *
 * <ol>
 *   <li><b>Quién sos</b> (autenticación): cada request trae un token JWT en el header
 *       {@code Authorization: Bearer ...}. Spring valida la firma y la expiración; si falta
 *       o es inválido responde 401.</li>
 *   <li><b>Qué podés hacer</b> (autorización por rol): el token lleva el claim {@code rol}, que
 *       se convierte en la autoridad {@code ROLE_ADMINISTRADOR} o {@code ROLE_EMPLEADO}.
 *       Las reglas por URL están acá; las de cada endpoint, en {@code @PreAuthorize} sobre el
 *       método del controller. Si el rol no alcanza responde 403.</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    static final String CLAIM_ROL = "rol";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                // API sin cookies ni sesión: el token viaja en cada request, así que CSRF no aplica
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos: estado del sistema, login, registro y la lista de sucursales
                        // (la pide el formulario de registro)
                        .requestMatchers("/health", "/auth/login", "/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sucursales/**").permitAll()
                        // Todo lo demás exige estar logueado; el rol se controla en cada endpoint
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesDesdeToken())))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribirError(res, objectMapper,
                                HttpStatus.UNAUTHORIZED, "Iniciá sesión para continuar."))
                        .accessDeniedHandler((req, res, ex) -> escribirError(res, objectMapper,
                                HttpStatus.FORBIDDEN, "Tu rol no tiene permiso para esta operación.")));
        return http.build();
    }

    /** Convierte el claim {@code rol} del token en la autoridad ROLE_xxx que usa hasRole(). */
    private JwtAuthenticationConverter rolesDesdeToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    SecretKey jwtKey(JwtProperties props) {
        return new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Las contraseñas se guardan con hash BCrypt, nunca en texto plano. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Errores 401/403 con el mismo formato (ProblemDetail) que el resto de la API. */
    private static void escribirError(HttpServletResponse res, ObjectMapper mapper, HttpStatus status,
                                      String detalle) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(res.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detalle));
    }
}
