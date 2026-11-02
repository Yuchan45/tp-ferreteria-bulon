package com.bulon.stock.usuarios;

import com.bulon.stock.auth.AuthDTOs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Editar y desactivar proveedores y usuarios, sobre los datos del seeder. */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:edicion_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EdicionUsuariosYProveedoresTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void elAdministradorEditaUnProveedorYElEmpleadoNo() throws Exception {
        JsonNode herramientas = proveedor("Herramientas");
        long id = herramientas.get("id").asLong();
        String body = """
                {"razonSocial":"Herramientas Patagonia S.A.","cuit":"%s","telefono":"0299-444-0000",
                 "email":"ventas@patagonia.com.ar","contacto":"Nueva Persona","activo":true}"""
                .formatted(herramientas.get("cuit").asText());

        mvc.perform(conToken(put("/api/proveedores/" + id), "ventas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(put("/api/proveedores/" + id), "admin").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Herramientas Patagonia S.A."))
                .andExpect(jsonPath("$.contacto").value("Nueva Persona"));
    }

    @Test
    void noSePuedeUsarElCuitDeOtroProveedor() throws Exception {
        JsonNode herramientas = proveedor("Herramientas");
        String body = """
                {"razonSocial":"Herramientas Patagonia S.R.L.","cuit":"%s","activo":true}"""
                .formatted(proveedor("Bulonera").get("cuit").asText());

        mvc.perform(conToken(put("/api/proveedores/" + herramientas.get("id").asLong()), "admin")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void aUnProveedorInactivoNoSeLeCompra() throws Exception {
        JsonNode bulonera = proveedor("Bulonera");
        ((com.fasterxml.jackson.databind.node.ObjectNode) bulonera).put("activo", false);
        mvc.perform(conToken(put("/api/proveedores/" + bulonera.get("id").asLong()), "admin")
                        .contentType(MediaType.APPLICATION_JSON).content(bulonera.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // Lo que vende la bulonera (BUL-001) ya no se le puede comprar a ella
        long productoId = producto("BUL-001").get("id").asLong();
        String compra = """
                {"productoId":%d,"sucursalId":%d,"tipo":"INGRESO_COMPRA","cantidad":10,"proveedorId":%d}"""
                .formatted(productoId, sucursalId(), bulonera.get("id").asLong());
        mvc.perform(conToken(post("/api/movimientos"), "ventas").contentType(MediaType.APPLICATION_JSON).content(compra))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("está inactivo")));
    }

    @Test
    void unUsuarioDesactivadoNoPuedeEntrarYAlReactivarloSi() throws Exception {
        long ventasId = usuario("ventas").get("id").asLong();

        editarUsuario(ventasId, "ventas", null, false).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        login("ventas", "empleado").andExpect(status().isUnauthorized());

        editarUsuario(ventasId, "ventas", null, true).andExpect(status().isOk());
        login("ventas", "empleado").andExpect(status().isOk());
    }

    @Test
    void elAdministradorPuedeResetearLaContrasena() throws Exception {
        long ventasId = usuario("ventas").get("id").asLong();

        editarUsuario(ventasId, "ventas", "nueva-clave-123", true).andExpect(status().isOk());

        login("ventas", "empleado").andExpect(status().isUnauthorized());
        login("ventas", "nueva-clave-123").andExpect(status().isOk());
    }

    @Test
    void elAdministradorNoPuedeDesactivarseNiQuitarseElRol() throws Exception {
        long adminId = usuario("admin").get("id").asLong();
        String sinRol = """
                {"nombre":"Administrador General","rol":"EMPLEADO","sucursalId":%d,"activo":true}""".formatted(sucursalId());

        editarUsuario(adminId, "admin", null, false).andExpect(status().isUnprocessableEntity());
        mvc.perform(conToken(put("/api/usuarios/" + adminId), "admin").contentType(MediaType.APPLICATION_JSON).content(sinRol))
                .andExpect(status().isUnprocessableEntity());
    }

    /** Edita nombre y estado de un usuario del seeder manteniendo su rol y sucursal. */
    private ResultActions editarUsuario(long id, String username, String password, boolean activo) throws Exception {
        JsonNode actual = usuario(username);
        String body = json.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
            put("nombre", actual.get("nombre").asText());
            put("email", actual.get("email").isNull() ? null : actual.get("email").asText());
            put("password", password);
            put("rol", actual.get("rol").asText());
            put("sucursalId", actual.get("sucursalId").isNull() ? null : actual.get("sucursalId").asLong());
            put("activo", activo);
        }});
        return mvc.perform(conToken(put("/api/usuarios/" + id), "admin").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode usuario(String username) throws Exception {
        return buscar("/api/usuarios", "username", username, true);
    }

    private JsonNode proveedor(String prefijo) throws Exception {
        return buscar("/api/proveedores", "razonSocial", prefijo, false);
    }

    private JsonNode producto(String codigo) throws Exception {
        return buscar("/api/productos", "codigo", codigo, true);
    }

    private JsonNode buscar(String url, String campo, String valor, boolean exacto) throws Exception {
        JsonNode lista = json.readTree(mvc.perform(conToken(get(url), "admin")).andReturn().getResponse().getContentAsString());
        for (JsonNode item : lista) {
            String texto = item.get(campo).asText();
            if (exacto ? texto.equals(valor) : texto.startsWith(valor)) {
                return item;
            }
        }
        throw new AssertionError("No se encontró " + valor + " en " + url);
    }

    private long sucursalId() throws Exception {
        return json.readTree(mvc.perform(get("/api/sucursales").contextPath("/api")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asLong();
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contextPath("/api").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new AuthDTOs.LoginRequest(username, password))));
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder request, String username) throws Exception {
        String password = username.equals("admin") ? "admin" : "empleado";
        String token = json.readTree(login(username, password).andReturn().getResponse().getContentAsString()).get("token").asText();
        return request.contextPath("/api").header("Authorization", "Bearer " + token);
    }
}
