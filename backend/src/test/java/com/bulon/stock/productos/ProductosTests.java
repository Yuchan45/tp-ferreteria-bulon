package com.bulon.stock.productos;

import com.bulon.stock.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductosTests extends ApiTest {

    private long crearProveedor(String razonSocial) throws Exception {
        return leer(post(ADMIN, "/proveedores", Map.of("razonSocial", razonSocial))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    @Test
    void productoConVariosProveedoresYUnPreferido() throws Exception {
        long a = crearProveedor("Proveedor A");
        long b = crearProveedor("Proveedor B");
        Map<String, Object> body = producto(codigoNuevo(), 5, 10);
        body.put("proveedorIds", List.of(a));
        body.put("proveedorPreferidoId", b);

        JsonNode creado = leer(post(ADMIN, "/productos", body).andExpect(status().isCreated()));

        assertThat(lista(creado.get("proveedorIds"))).extracting(JsonNode::asLong).containsExactly(a, b);
        assertThat(creado.get("proveedorPreferidoId").asLong()).isEqualTo(b);
        assertThat(creado.get("estadoStock").asText()).isEqualTo("BAJO_MINIMO");
        assertThat(lista(creado.get("existencias"))).extracting(e -> e.get("ubicacion").asText())
                .containsExactly("Sucursal", "Depósito");
    }

    @Test
    void actualizarConElCodigoDeOtroProductoDa422() throws Exception {
        String codigoExistente = codigoNuevo();
        post(ADMIN, "/productos", producto(codigoExistente, 0, 0)).andExpect(status().isCreated());
        long id = crearProducto(0, 0);

        put(ADMIN, "/productos/" + id, producto(codigoExistente, 0, 0))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Ya existe un producto con código " + codigoExistente));
    }

    @Test
    void elPuntoDePedidoNoPuedeSerMenorQueElMinimo() throws Exception {
        post(ADMIN, "/productos", producto(codigoNuevo(), 10, 5)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ordenarPorUnCampoInexistenteDa400() throws Exception {
        get(ADMIN, "/productos?sort=noExiste").andExpect(status().isBadRequest());
        get(ADMIN, "/movimientos?sort=noExiste").andExpect(status().isBadRequest());
    }

    @Test
    void camposObligatoriosDan400() throws Exception {
        post(ADMIN, "/productos", Map.of("codigo", "")).andExpect(status().isBadRequest());
    }

    @Test
    void listadoPaginadoFiltraPorTextoYPorPendientesDeReposicion() throws Exception {
        String codigo = codigoNuevo();
        long id = leer(post(ADMIN, "/productos", producto(codigo, 2, 5))).get("id").asLong();
        mover(id, "INGRESO_COMPRA", 20, deposito());

        JsonNode porTexto = leer(get(ADMIN, "/productos?q={q}", codigo.toLowerCase()));
        mover(id, "SALIDA_VENTA", 16, deposito());
        JsonNode pendientes = leer(get(ADMIN, "/productos?reposicion=true&size=500"));

        Map<String, Object> inactivo = producto(codigoNuevo(), 2, 5);
        inactivo.put("activo", false);
        long idInactivo = leer(post(ADMIN, "/productos", inactivo)).get("id").asLong();
        JsonNode pendientesConInactivo = leer(get(ADMIN, "/productos?reposicion=true&size=500"));

        assertThat(lista(pendientesConInactivo.get("content"))).noneMatch(p -> p.get("id").asLong() == idInactivo);
        assertThat(porTexto.at("/page/totalElements").asInt()).isEqualTo(1);
        assertThat(porTexto.at("/content/0/stockTotal").asInt()).isEqualTo(20);
        assertThat(lista(pendientes.get("content"))).filteredOn(p -> p.get("id").asLong() == id)
                .singleElement().satisfies(p -> assertThat(p.get("estadoStock").asText()).isEqualTo("PUNTO_PEDIDO"));
    }

    @Test
    void proveedorValidaCuitYNoSeBorraSiTieneProductos() throws Exception {
        post(ADMIN, "/proveedores", Map.of("razonSocial", "Mal", "cuit", "123")).andExpect(status().isBadRequest());
        String cuit = "30-%08d-9".formatted(System.nanoTime() % 100_000_000);
        post(ADMIN, "/proveedores", Map.of("razonSocial", "Uno", "cuit", cuit)).andExpect(status().isCreated());
        post(ADMIN, "/proveedores", Map.of("razonSocial", "Dos", "cuit", cuit))
                .andExpect(status().isUnprocessableEntity());

        long proveedor = crearProveedor("En uso");
        Map<String, Object> body = producto(codigoNuevo(), 0, 0);
        body.put("proveedorPreferidoId", proveedor);
        post(ADMIN, "/productos", body).andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.delete("/proveedores/" + proveedor).with(httpBasic(ADMIN, "admin123")))
                .andExpect(status().isConflict());
        long libre = crearProveedor("Libre");
        mvc.perform(MockMvcRequestBuilders.delete("/proveedores/" + libre).with(httpBasic(ADMIN, "admin123")))
                .andExpect(status().isNoContent());
    }
}
