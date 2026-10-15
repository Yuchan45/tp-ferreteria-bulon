package com.bulon.stock;

import com.bulon.stock.seed.DataSeeder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de API: misma configuración (un solo contexto de Spring), usuarios del seeder
 * autenticados con HTTP Basic real y helpers para armar escenarios con productos propios de cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTest {

    protected static final String ADMIN = "admin";
    protected static final String DEPOSITO = "deposito";
    protected static final String VENTAS = "ventas";

    private static final AtomicInteger SECUENCIA = new AtomicInteger();
    private static final Map<String, String> PASSWORDS = new HashMap<>();

    static {
        DataSeeder.USUARIOS.forEach(u -> PASSWORDS.put(u.username(), u.password()));
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected ResultActions get(String usuario, String url, Object... variables) throws Exception {
        return mvc.perform(como(usuario, MockMvcRequestBuilders.get(url, variables)));
    }

    protected ResultActions post(String usuario, String url, Object body) throws Exception {
        return mvc.perform(como(usuario, MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))));
    }

    protected ResultActions put(String usuario, String url, Object body) throws Exception {
        return mvc.perform(como(usuario, MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))));
    }

    protected JsonNode leer(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsByteArray());
    }

    protected long ubicacion(String nombre) throws Exception {
        for (JsonNode u : leer(get(ADMIN, "/ubicaciones"))) {
            if (u.get("nombre").asText().equals(nombre)) {
                return u.get("id").asLong();
            }
        }
        throw new IllegalStateException("No existe la ubicación " + nombre);
    }

    protected long sucursal() throws Exception {
        return ubicacion(DataSeeder.SUCURSAL);
    }

    protected long deposito() throws Exception {
        return ubicacion(DataSeeder.DEPOSITO);
    }

    protected long categoriaGeneral() throws Exception {
        for (JsonNode c : leer(get(ADMIN, "/categorias"))) {
            if (c.get("nombre").asText().equals("General")) {
                return c.get("id").asLong();
            }
        }
        throw new IllegalStateException("Falta la categoría General");
    }

    protected String codigoNuevo() {
        return "T-" + SECUENCIA.incrementAndGet() + "-" + System.nanoTime() % 100_000;
    }

    protected Map<String, Object> producto(String codigo, int stockMinimo, int puntoPedido) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", codigo);
        body.put("nombre", "Producto de prueba " + codigo);
        body.put("categoriaId", categoriaGeneral());
        body.put("unidadMedida", "UNIDAD");
        body.put("stockMinimo", stockMinimo);
        body.put("puntoPedido", puntoPedido);
        return body;
    }

    protected long crearProducto(int stockMinimo, int puntoPedido) throws Exception {
        return leer(post(ADMIN, "/productos", producto(codigoNuevo(), stockMinimo, puntoPedido))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    protected ResultActions mover(String usuario, long productoId, String tipo, int cantidad, long ubicacionId,
                                  Long destinoId, String motivo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productoId", productoId);
        body.put("tipo", tipo);
        body.put("cantidad", cantidad);
        body.put("ubicacionId", ubicacionId);
        body.put("ubicacionDestinoId", destinoId);
        body.put("motivo", motivo);
        return post(usuario, "/movimientos", body);
    }

    protected ResultActions mover(long productoId, String tipo, int cantidad, long ubicacionId) throws Exception {
        return mover(ADMIN, productoId, tipo, cantidad, ubicacionId, null, null);
    }

    /** Stock del producto por nombre de ubicación, más la clave "total". */
    protected Map<String, Integer> stock(long productoId) throws Exception {
        JsonNode producto = leer(get(ADMIN, "/productos/{id}", productoId));
        Map<String, Integer> stock = new HashMap<>();
        producto.get("existencias").forEach(e -> stock.put(e.get("ubicacion").asText(), e.get("cantidad").asInt()));
        stock.put("total", producto.get("stockTotal").asInt());
        return stock;
    }

    protected static List<JsonNode> lista(JsonNode nodo) {
        List<JsonNode> elementos = new ArrayList<>();
        nodo.forEach(elementos::add);
        return elementos;
    }

    private static MockHttpServletRequestBuilder como(String usuario, MockHttpServletRequestBuilder request) {
        return usuario == null ? request : request.with(httpBasic(usuario, PASSWORDS.get(usuario)));
    }
}
