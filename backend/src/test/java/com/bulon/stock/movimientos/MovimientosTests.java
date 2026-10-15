package com.bulon.stock.movimientos;

import com.bulon.stock.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovimientosTests extends ApiTest {

    @Test
    void ingresoSumaSoloEnLaUbicacionIndicada() throws Exception {
        long id = crearProducto(0, 0);

        mover(id, "INGRESO_COMPRA", 10, deposito()).andExpect(status().isCreated());

        assertThat(stock(id)).isEqualTo(Map.of("Sucursal", 0, "Depósito", 10, "total", 10));
    }

    @Test
    void ventaRestaYSeRechazaSiNoAlcanzaEnEsaUbicacion() throws Exception {
        long id = crearProducto(0, 0);
        mover(id, "INGRESO_COMPRA", 5, sucursal());
        mover(id, "INGRESO_COMPRA", 50, deposito());

        mover(id, "SALIDA_VENTA", 3, sucursal()).andExpect(status().isCreated());
        JsonNode error = leer(mover(id, "SALIDA_VENTA", 5, sucursal()).andExpect(status().isUnprocessableEntity()));

        assertThat(error.get("detail").asText()).endsWith("en Sucursal: disponible 2, solicitado 5");
        assertThat(stock(id)).isEqualTo(Map.of("Sucursal", 2, "Depósito", 50, "total", 52));
    }

    @Test
    void roturaRestaYDevolucionSuma() throws Exception {
        long id = crearProducto(0, 0);
        mover(id, "INGRESO_COMPRA", 10, deposito());

        mover(id, "ROTURA_PERDIDA", 4, deposito()).andExpect(status().isCreated());
        mover(id, "DEVOLUCION", 1, deposito()).andExpect(status().isCreated());

        assertThat(stock(id).get("total")).isEqualTo(7);
    }

    @Test
    void ajusteExigeMotivoYAdmiteCantidadNegativa() throws Exception {
        long id = crearProducto(0, 0);
        mover(id, "INGRESO_COMPRA", 10, deposito());

        JsonNode sinMotivo = leer(mover(ADMIN, id, "AJUSTE", -3, deposito(), null, null)
                .andExpect(status().isUnprocessableEntity()));
        mover(ADMIN, id, "AJUSTE", -3, deposito(), null, "Recuento: faltan 3").andExpect(status().isCreated());
        mover(ADMIN, id, "AJUSTE", -8, deposito(), null, "Recuento").andExpect(status().isUnprocessableEntity());

        assertThat(sinMotivo.get("detail").asText()).isEqualTo("El motivo es obligatorio en un ajuste");
        assertThat(stock(id).get("Depósito")).isEqualTo(7);
    }

    @Test
    void trasladoRestaEnOrigenYSumaEnDestinoOFallaSinTocarNada() throws Exception {
        long id = crearProducto(0, 0);
        mover(id, "INGRESO_COMPRA", 10, deposito());

        mover(ADMIN, id, "TRASLADO", 4, deposito(), sucursal(), null).andExpect(status().isCreated());
        mover(ADMIN, id, "TRASLADO", 7, deposito(), sucursal(), null).andExpect(status().isUnprocessableEntity());
        mover(ADMIN, id, "TRASLADO", 1, deposito(), null, null).andExpect(status().isUnprocessableEntity());
        mover(ADMIN, id, "TRASLADO", 1, deposito(), deposito(), null).andExpect(status().isUnprocessableEntity());

        assertThat(stock(id)).isEqualTo(Map.of("Sucursal", 4, "Depósito", 6, "total", 10));
    }

    @Test
    void soloElTrasladoLlevaDestinoYLaCantidadDebeSerPositiva() throws Exception {
        long id = crearProducto(0, 0);

        mover(ADMIN, id, "INGRESO_COMPRA", 5, deposito(), sucursal(), null).andExpect(status().isUnprocessableEntity());
        mover(id, "INGRESO_COMPRA", 0, deposito()).andExpect(status().isUnprocessableEntity());
        mover(id, "SALIDA_VENTA", -2, deposito()).andExpect(status().isUnprocessableEntity());

        assertThat(stock(id).get("total")).isZero();
    }

    @Test
    void elResponsableEsElUsuarioAutenticadoAunqueElBodyDigaOtraCosa() throws Exception {
        long id = crearProducto(0, 0);

        mover(DEPOSITO, id, "INGRESO_COMPRA", 3, deposito(), null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value(DEPOSITO));

        get(ADMIN, "/movimientos?productoId={id}", id)
                .andExpect(jsonPath("$.content[0].usuario").value(DEPOSITO));
    }

    @Test
    void losMovimientosNoSePuedenModificarNiBorrar() throws Exception {
        long id = crearProducto(0, 0);
        long movimiento = leer(mover(id, "INGRESO_COMPRA", 3, deposito())).get("id").asLong();

        put(ADMIN, "/movimientos/" + movimiento, Map.of("cantidad", 300)).andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.delete("/movimientos/" + movimiento).with(httpBasic(ADMIN, "admin123")))
                .andExpect(status().isNotFound());

        assertThat(stock(id).get("total")).isEqualTo(3);
    }

    @Test
    void productoInactivoNoAdmiteMovimientos() throws Exception {
        String codigo = codigoNuevo();
        Map<String, Object> body = producto(codigo, 0, 0);
        body.put("activo", false);
        long id = leer(post(ADMIN, "/productos", body)).get("id").asLong();

        mover(id, "INGRESO_COMPRA", 1, deposito()).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void historialFiltraPorTipoYUbicacionYPagina() throws Exception {
        long id = crearProducto(0, 0);
        mover(id, "INGRESO_COMPRA", 10, deposito());
        mover(id, "INGRESO_COMPRA", 10, deposito());
        mover(ADMIN, id, "TRASLADO", 5, deposito(), sucursal(), null);

        JsonNode traslados = leer(get(ADMIN, "/movimientos?productoId={id}&tipo=TRASLADO", id));
        JsonNode enSucursal = leer(get(ADMIN, "/movimientos?productoId={id}&ubicacionId={u}", id, sucursal()));
        JsonNode pagina = leer(get(ADMIN, "/movimientos?productoId={id}&size=2&page=1", id));
        JsonNode hoy = leer(get(ADMIN, "/movimientos?productoId={id}&desde={d}&hasta={d}", id,
                java.time.LocalDate.now(), java.time.LocalDate.now()));

        assertThat(traslados.at("/page/totalElements").asInt()).isEqualTo(1);
        assertThat(enSucursal.at("/content/0/ubicacionDestino").asText()).isEqualTo("Sucursal");
        assertThat(enSucursal.at("/page/totalElements").asInt()).isEqualTo(1);
        assertThat(pagina.at("/page/totalElements").asInt()).isEqualTo(3);
        assertThat(pagina.get("content")).hasSize(1);
        assertThat(hoy.at("/page/totalElements").asInt()).isEqualTo(3);
    }

    @Test
    void ventasSimultaneasNuncaDejanStockNegativoNiDescuadrado() throws Exception {
        long id = crearProducto(0, 0);
        long sucursal = sucursal();
        mover(id, "INGRESO_COMPRA", 10, sucursal);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> ventas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ventas.add(() -> mvc.perform(MockMvcRequestBuilders.post("/movimientos")
                            .with(httpBasic(ADMIN, "admin123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("productoId", id, "tipo", "SALIDA_VENTA",
                                    "cantidad", 3, "ubicacionId", sucursal))))
                    .andReturn().getResponse().getStatus());
        }
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> f : pool.invokeAll(ventas)) {
            estados.add(f.get());
        }
        pool.shutdown();

        long exitosas = estados.stream().filter(s -> s == 201).count();
        assertThat(estados).allMatch(s -> s == 201 || s == 409 || s == 422);
        assertThat(exitosas).isBetween(1L, 3L);
        assertThat(stock(id).get("Sucursal")).isEqualTo(10 - 3 * (int) exitosas);
        assertThat(leer(get(ADMIN, "/movimientos?productoId={id}&tipo=SALIDA_VENTA", id))
                .at("/page/totalElements").asLong()).isEqualTo(exitosas);
    }
}
