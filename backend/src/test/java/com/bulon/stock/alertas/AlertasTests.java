package com.bulon.stock.alertas;

import com.bulon.stock.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertasTests extends ApiTest {

    private List<JsonNode> alertasDe(long productoId) throws Exception {
        return lista(leer(get(ADMIN, "/alertas?abiertas=false&size=1000")).get("content")).stream()
                .filter(a -> a.get("productoId").asLong() == productoId)
                .toList();
    }

    @Test
    void alertaSeAbreEnElPuntoDePedidoEscalaBajoElMinimoYSeCierraAlReponer() throws Exception {
        long id = crearProducto(2, 5);
        mover(id, "INGRESO_COMPRA", 10, deposito());
        assertThat(alertasDe(id)).isEmpty();

        mover(id, "SALIDA_VENTA", 6, deposito());
        assertThat(alertasDe(id)).singleElement().satisfies(a -> {
            assertThat(a.get("estado").asText()).isEqualTo("PUNTO_PEDIDO");
            assertThat(a.get("stock").asInt()).isEqualTo(4);
            assertThat(a.get("resueltaEn").isNull()).isTrue();
        });

        mover(id, "SALIDA_VENTA", 2, deposito());
        assertThat(alertasDe(id)).singleElement().satisfies(a -> {
            assertThat(a.get("estado").asText()).isEqualTo("BAJO_MINIMO");
            assertThat(a.get("stock").asInt()).isEqualTo(2);
        });

        mover(id, "INGRESO_COMPRA", 10, deposito());
        assertThat(alertasDe(id)).singleElement()
                .satisfies(a -> assertThat(a.get("resueltaEn").isNull()).isFalse());
        assertThat(lista(leer(get(ADMIN, "/alertas?size=1000")).get("content")))
                .noneMatch(a -> a.get("productoId").asLong() == id);
    }

    @Test
    void unTrasladoNoCambiaElTotalNiAbreAlertas() throws Exception {
        long id = crearProducto(2, 5);
        mover(id, "INGRESO_COMPRA", 10, deposito());

        mover(ADMIN, id, "TRASLADO", 9, deposito(), sucursal(), null);

        assertThat(alertasDe(id)).isEmpty();
    }
}
