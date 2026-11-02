package com.bulon.stock.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración de los tokens: {@code app.jwt.*} en application.yml. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiracionHoras) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Falta JWT_SECRET en backend/.env (mínimo 32 caracteres). Ver backend/.env.example");
        }
    }
}
