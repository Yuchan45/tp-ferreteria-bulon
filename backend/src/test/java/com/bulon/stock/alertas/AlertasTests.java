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
    void editarLosUmbralesODesactivarElProductoReevaluaSuAlerta() throws Exception {
        String codigo = codigoNuevo();
        long id = leer(post(ADMIN, "/productos", producto(codigo, 2, 5))).get("id").asLong();
        mover(id, "INGRESO_COMPRA", 4, deposito());
        assertThat(alertasDe(id)).singleElement()
                .satisfies(a -> assertThat(a.get("estado").asText()).isEqualTo("PUNTO_PEDIDO"));

        put(ADMIN, "/productos/" + id, producto(codigo, 2, 3));
        assertThat(alertasDe(id)).singleElement()
                .satisfies(a -> assertThat(a.get("resueltaEn").isNull()).isFalse());

        put(ADMIN, "/productos/" + id, producto(codigo, 4, 8));
        assertThat(alertasDe(id)).filteredOn(a -> a.get("resueltaEn").isNull()).singleElement()
                .satisfies(a -> assertThat(a.get("estado").asText()).isEqualTo("BAJO_MINIMO"));

        var inactivo = producto(codigo, 4, 8);
        inactivo.put("activo", false);
        put(ADMIN, "/productos/" + id, inactivo);
        assertThat(alertasDe(id)).allMatch(a -> !a.get("resueltaEn").isNull());
    }

    @Test
    void unTrasladoNoCambiaElTotalNiAbreAlertas() throws Exception {
        long id = crearProducto(2, 5);
        mover(id, "INGRESO_COMPRA", 10, deposito());

        mover(ADMIN, id, "TRASLADO", 9, deposito(), sucursal(), null);

        assertThat(alertasDe(id)).isEmpty();
    }
}
