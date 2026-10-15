package com.bulon.stock.compras;

import com.bulon.stock.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrdenesCompraTests extends ApiTest {

    @Test
    void sugiereReponerHastaElDobleDelPuntoDePedidoAgrupadoPorProveedorPreferido() throws Exception {
        long proveedor = leer(post(ADMIN, "/proveedores", Map.of("razonSocial", "Sugerido S.A."))).get("id").asLong();
        Map<String, Object> body = producto(codigoNuevo(), 4, 10);
        body.put("proveedorPreferidoId", proveedor);
        long id = leer(post(ADMIN, "/productos", body)).get("id").asLong();
        mover(id, "INGRESO_COMPRA", 6, deposito());

        JsonNode grupo = lista(leer(get(DEPOSITO, "/ordenes-compra/sugerencias"))).stream()
                .filter(g -> g.get("proveedorId").asLong() == proveedor).findFirst().orElseThrow();

        assertThat(grupo.get("proveedor").asText()).isEqualTo("Sugerido S.A.");
        assertThat(grupo.at("/items/0/productoId").asLong()).isEqualTo(id);
        assertThat(grupo.at("/items/0/cantidadSugerida").asInt()).isEqualTo(14);
    }

    @Test
    void cicloDeVidaBorradorEnviadaRecibidaIngresaElStock() throws Exception {
        long proveedor = leer(post(ADMIN, "/proveedores", Map.of("razonSocial", "Ciclo S.R.L."))).get("id").asLong();
        long id = crearProducto(0, 0);
        long orden = leer(post(DEPOSITO, "/ordenes-compra", Map.of("proveedorId", proveedor,
                "items", List.of(Map.of("productoId", id, "cantidad", 12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))).get("id").asLong();
        Map<String, Object> enDeposito = Map.of("ubicacionId", deposito());

        post(DEPOSITO, "/ordenes-compra/" + orden + "/recibir", enDeposito).andExpect(status().isUnprocessableEntity());
        post(DEPOSITO, "/ordenes-compra/" + orden + "/enviar", Map.of()).andExpect(jsonPath("$.estado").value("ENVIADA"));
        post(DEPOSITO, "/ordenes-compra/" + orden + "/recibir", enDeposito)
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.ubicacionRecepcion").value("Depósito"));
        post(DEPOSITO, "/ordenes-compra/" + orden + "/cancelar", Map.of()).andExpect(status().isUnprocessableEntity());

        assertThat(stock(id).get("Depósito")).isEqualTo(12);
        get(ADMIN, "/movimientos?productoId={id}", id)
                .andExpect(jsonPath("$.content[0].tipo").value("INGRESO_COMPRA"))
                .andExpect(jsonPath("$.content[0].usuario").value(DEPOSITO))
                .andExpect(jsonPath("$.content[0].observacion").value("Recepción de orden de compra #" + orden));
    }

    @Test
    void elVendedorNoGestionaOrdenesDeCompra() throws Exception {
        post(VENTAS, "/ordenes-compra", Map.of("proveedorId", 1, "items", List.of()))
                .andExpect(status().isForbidden());
    }
}
