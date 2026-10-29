package com.bulon.stock.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login, registro y permisos por rol, sobre los usuarios del seeder (admin/admin, empleados/empleado). */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:seguridad_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SeguridadTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void sinTokenLaApiRespondeNoAutorizado() throws Exception {
        mvc.perform(api(get("/api/productos")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Iniciá sesión para continuar."));
    }

    @Test
    void losEndpointsPublicosNoPidenToken() throws Exception {
        mvc.perform(api(get("/api/health"))).andExpect(status().isOk());
        mvc.perform(api(get("/api/sucursales"))).andExpect(status().isOk());
    }

    @Test
    void loginCorrectoDevuelveTokenYUsuario() throws Exception {
        JsonNode sesion = login("admin", "admin");
        assertThat(sesion.get("token").asText()).isNotBlank();
        assertThat(sesion.at("/usuario/rol").asText()).isEqualTo("ADMINISTRADOR");
        assertThat(sesion.at("/usuario").has("passwordHash")).isFalse();
    }

    @Test
    void loginIncorrectoNoDiceQueFallo() throws Exception {
        for (String[] credenciales : new String[][]{{"admin", "otra-clave"}, {"no-existe", "empleado"}}) {
            mvc.perform(api(post("/api/auth/login"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new AuthDTOs.LoginRequest(credenciales[0], credenciales[1]))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));
        }
    }

    @Test
    void unTokenAdulteradoSeRechaza() throws Exception {
        String token = login("ventas", "empleado").get("token").asText();
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";
        mvc.perform(api(get("/api/productos")).header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elEmpleadoConsultaPeroNoAdministra() throws Exception {
        String empleado = token("ventas");

        mvc.perform(conToken(get("/api/productos"), empleado)).andExpect(status().isOk());
        mvc.perform(conToken(post("/api/productos"), empleado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"X-1\",\"nombre\":\"Prueba\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Tu rol no tiene permiso para esta operación."));
        mvc.perform(conToken(get("/api/usuarios"), empleado)).andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorPuedeAdministrar() throws Exception {
        String admin = token("admin");

        mvc.perform(conToken(get("/api/usuarios"), admin)).andExpect(status().isOk());
        mvc.perform(conToken(post("/api/productos"), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"X-1\",\"nombre\":\"Prueba\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void elRegistroCreaUnEmpleadoAunqueSePidaOtroRol() throws Exception {
        String body = """
                {"username":"nuevo","nombre":"Empleado Nuevo","password":"clave-segura",
                 "sucursalId":%d,"rol":"ADMINISTRADOR"}""".formatted(sucursalId());

        String respuesta = mvc.perform(api(post("/api/auth/registro")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rol").value("EMPLEADO"))
                .andReturn().getResponse().getContentAsString();

        String token = json.readTree(respuesta).get("token").asText();
        mvc.perform(conToken(get("/api/auth/yo"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("nuevo"));
    }

    @Test
    void elResponsableDelMovimientoSaleDelToken() throws Exception {
        String empleado = token("ventas");
        long empleadoId = login("ventas", "empleado").at("/usuario/id").asLong();
        long adminId = login("admin", "admin").at("/usuario/id").asLong();
        long productoId = json.readTree(mvc.perform(conToken(get("/api/stock?sucursalId=" + sucursalId()), empleado))
                .andReturn().getResponse().getContentAsString()).get(0).get("productoId").asLong();

        // Aunque el body diga que lo registró el admin, queda a nombre del usuario del token
        String body = """
                {"productoId":%d,"sucursalId":%d,"usuarioId":%d,"tipo":"SALIDA_VENTA","cantidad":1}"""
                .formatted(productoId, sucursalId(), adminId);
        mvc.perform(conToken(post("/api/movimientos"), empleado).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(empleadoId));
    }

    private long sucursalId() throws Exception {
        return json.readTree(mvc.perform(api(get("/api/sucursales"))).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asLong();
    }

    private String token(String username) throws Exception {
        return login(username, username.equals("admin") ? "admin" : "empleado").get("token").asText();
    }

    private JsonNode login(String username, String password) throws Exception {
        String respuesta = mvc.perform(api(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AuthDTOs.LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder request, String token) {
        return api(request).header("Authorization", "Bearer " + token);
    }

    /** La API vive bajo el context-path /api. */
    private static MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder request) {
        return request.contextPath("/api");
    }
}
