package com.bulon.stock.reportes;

import com.bulon.stock.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportesTests extends ApiTest {

    @Test
    void dashboardResumeElInventario() throws Exception {
        JsonNode panel = leer(get(VENTAS, "/dashboard").andExpect(status().isOk()));

        assertThat(panel.get("productosActivos").asLong()).isGreaterThanOrEqualTo(25);
        assertThat(panel.get("pendientesReposicion").asLong()).isGreaterThanOrEqualTo(panel.get("bajoMinimo").asLong());
        assertThat(lista(panel.get("unidadesPorUbicacion"))).extracting(u -> u.get("ubicacion").asText())
                .containsExactly("Sucursal", "Depósito");
        assertThat(panel.get("ultimosMovimientos")).isNotEmpty();
    }

    @Test
    void rotacionCuentaLasVentasDelPeriodoYSugierePuntoDePedido() throws Exception {
        long id = crearProducto(3, 3);
        mover(id, "INGRESO_COMPRA", 1000, sucursal());
        mover(id, "SALIDA_VENTA", 600, sucursal());

        JsonNode fila = lista(leer(get(ADMIN, "/reportes/rotacion?limite=100"))).stream()
                .filter(r -> r.get("productoId").asLong() == id).findFirst().orElseThrow();

        assertThat(fila.get("unidadesVendidas").asLong()).isEqualTo(600);
        // 600 vendidas / 30 días × 7 días de entrega + 3 de stock mínimo
        assertThat(fila.get("puntoPedidoSugerido").asInt()).isEqualTo(143);
    }

    @Test
    void exportaMovimientosEnPdfEInventarioEnExcel() throws Exception {
        MockHttpServletResponse pdf = get(ADMIN, "/reportes/movimientos?formato=PDF")
                .andExpect(status().isOk()).andReturn().getResponse();
        MockHttpServletResponse excel = get(ADMIN, "/reportes/inventario?formato=EXCEL")
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(new String(pdf.getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
        assertThat(pdf.getHeader("Content-Disposition")).startsWith("attachment; filename=\"movimientos-");
        assertThat(new String(excel.getContentAsByteArray(), 0, 2)).isEqualTo("PK");
        assertThat(excel.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
}
