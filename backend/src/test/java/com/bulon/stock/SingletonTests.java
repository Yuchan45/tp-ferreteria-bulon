package com.bulon.stock;

import com.bulon.stock.alertas.AlertaService;
import com.bulon.stock.stock.StockService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Patrón Singleton: no se escribe a mano, lo garantiza Spring. Cada bean (el pool de
 * conexiones, los servicios, los repositorios) existe una sola vez y se comparte en toda
 * la aplicación. Estos tests lo demuestran.
 */
@SpringBootTest
@ActiveProfiles("test")
class SingletonTests {

    @Autowired
    private ConfigurableApplicationContext contexto;
    @Autowired
    private DataSource dataSource;

    @Test
    void hayUnSoloPoolDeConexiones() {
        // Un solo DataSource en toda la aplicación, y es el pool de HikariCP
        assertThat(contexto.getBeansOfType(DataSource.class)).hasSize(1);
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        // Pedirlo de nuevo devuelve el mismo objeto, no uno nuevo
        assertThat(contexto.getBean(DataSource.class)).isSameAs(dataSource);
    }

    @Test
    void losServiciosSonLaMismaInstanciaEnTodaLaAplicacion() {
        assertThat(contexto.getBean(StockService.class)).isSameAs(contexto.getBean(StockService.class));
        assertThat(contexto.getBeanFactory().getBeanDefinition("stockService").getScope())
                .isIn(BeanDefinition.SCOPE_SINGLETON, "");   // "" = el alcance por defecto, singleton
        assertThat(contexto.getBeanFactory().isSingleton("alertaService")).isTrue();
        assertThat(contexto.getBean(AlertaService.class)).isSameAs(contexto.getBean(AlertaService.class));
    }
}
