package com.bulon.stock.seed;

import com.bulon.stock.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DataSeederTests extends ApiTest {

    private static final Set<String> BAJO_MINIMO = Set.of("BUL-002", "HER-003", "ELE-003", "PLO-002", "PIN-004", "PIN-005");
    private static final Set<String> PUNTO_PEDIDO = Set.of("HER-004", "ELE-001", "ELE-005", "PLO-003", "PLO-005");

    @Autowired
    private DataSeeder dataSeeder;

    private List<JsonNode> productosDelSeed() throws Exception {
        return lista(leer(get(ADMIN, "/productos?size=1000")).get("content")).stream()
                .filter(p -> !p.get("codigo").asText().startsWith("T-"))
                .toList();
    }

    @Test
    void cargaCatalogoConStockRepartidoEntreUbicaciones() throws Exception {
        List<JsonNode> productos = productosDelSeed();

        assertThat(productos).hasSize(25);
        assertThat(productos).filteredOn(p -> p.get("codigo").asText().equals("BUL-001")).singleElement()
                .satisfies(p -> {
                    assertThat(p.get("categoria").asText()).isEqualTo("Bulonería y fijaciones");
                    assertThat(p.at("/existencias/0/cantidad").asInt()).isEqualTo(135);
                    assertThat(p.at("/existencias/1/cantidad").asInt()).isEqualTo(595);
                });
    }

    @Test
    void dejaProductosBajoMinimoYEnPuntoDePedidoParaLasAlertas() throws Exception {
        List<JsonNode> productos = productosDelSeed();

        assertThat(productos).filteredOn(p -> p.get("estadoStock").asText().equals("BAJO_MINIMO"))
                .extracting(p -> p.get("codigo").asText()).containsExactlyInAnyOrderElementsOf(BAJO_MINIMO);
        assertThat(productos).filteredOn(p -> p.get("estadoStock").asText().equals("PUNTO_PEDIDO"))
                .extracting(p -> p.get("codigo").asText()).containsExactlyInAnyOrderElementsOf(PUNTO_PEDIDO);
    }

    @Test
    void elStockDeCadaProductoCoincideConSuHistorial() throws Exception {
        for (JsonNode producto : productosDelSeed()) {
            int segunMovimientos = 0;
            for (JsonNode m : leer(get(ADMIN, "/movimientos?productoId={id}&size=1000", producto.get("id").asLong()))
                    .get("content")) {
                int cantidad = m.get("cantidad").asInt();
                segunMovimientos += switch (m.get("tipo").asText()) {
                    case "SALIDA_VENTA", "ROTURA_PERDIDA" -> -cantidad;
                    case "TRASLADO" -> 0;
                    default -> cantidad;
                };
            }
            assertThat(producto.get("stockTotal").asInt()).as(producto.get("codigo").asText())
                    .isEqualTo(segunMovimientos);
        }
    }

    @Test
    void noDuplicaDatosSiSeEjecutaDeNuevo() throws Exception {
        int usuarios = leer(get(ADMIN, "/usuarios")).size();
        int proveedores = leer(get(ADMIN, "/proveedores")).size();

        dataSeeder.run(null);

        assertThat(leer(get(ADMIN, "/usuarios")).size()).isEqualTo(usuarios);
        assertThat(leer(get(ADMIN, "/proveedores")).size()).isEqualTo(proveedores);
        assertThat(productosDelSeed()).hasSize(25);
    }
}
