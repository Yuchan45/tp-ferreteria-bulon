package com.bulon.stock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Con Flyway activo (tests contra SQL Server) cada corrida arranca de una base vacía. */
@Configuration
@Profile("test")
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
class TestFlywayConfig {

    @Bean
    FlywayMigrationStrategy limpiarYMigrar() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
