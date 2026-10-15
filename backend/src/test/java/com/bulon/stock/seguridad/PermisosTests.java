package com.bulon.stock.seguridad;

import com.bulon.stock.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermisosTests extends ApiTest {

    @Test
    void sinCredencialesSoloResponde401SinDialogoDelNavegador() throws Exception {
        get(null, "/productos")
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        get(null, "/health").andExpect(status().isOk());
    }

    @Test
    void passwordIncorrectaDa401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/auth/me").with(httpBasic(ADMIN, "otra")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMeDevuelveElUsuarioYSuRol() throws Exception {
        get(VENTAS, "/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(VENTAS))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"));
    }

    @Test
    void vendedorRegistraVentasYDevolucionesPeroNoIngresosNiAjustes() throws Exception {
        long id = crearProducto(0, 0);
        mover(id, "INGRESO_COMPRA", 10, sucursal());

        mover(VENTAS, id, "SALIDA_VENTA", 2, sucursal(), null, null).andExpect(status().isCreated());
        mover(VENTAS, id, "DEVOLUCION", 1, sucursal(), null, null).andExpect(status().isCreated());
        mover(VENTAS, id, "INGRESO_COMPRA", 5, sucursal(), null, null).andExpect(status().isForbidden());
        mover(VENTAS, id, "AJUSTE", -1, sucursal(), null, "x").andExpect(status().isForbidden());
        mover(VENTAS, id, "TRASLADO", 1, sucursal(), deposito(), null).andExpect(status().isForbidden());
    }

    @Test
    void vendedorConsultaPeroNoModificaElCatalogo() throws Exception {
        get(VENTAS, "/productos").andExpect(status().isOk());
        post(VENTAS, "/productos", producto(codigoNuevo(), 0, 0)).andExpect(status().isForbidden());
        post(VENTAS, "/proveedores", Map.of("razonSocial", "X")).andExpect(status().isForbidden());
    }

    @Test
    void soloElAdministradorGestionaUsuarios() throws Exception {
        get(VENTAS, "/usuarios").andExpect(status().isForbidden());
        get(DEPOSITO, "/usuarios").andExpect(status().isForbidden());
        get(ADMIN, "/usuarios").andExpect(status().isOk());
    }

    @Test
    void usuarioCreadoPorElAdminIngresaYAlDesactivarloNo() throws Exception {
        String username = "u" + System.nanoTime() % 1_000_000;
        long id = leer(post(ADMIN, "/usuarios", Map.of("username", username, "nombre", "Nuevo",
                "rol", "VENDEDOR", "password", "secreta1")).andExpect(status().isCreated())).get("id").asLong();

        mvc.perform(MockMvcRequestBuilders.get("/auth/me").with(httpBasic(username, "secreta1")))
                .andExpect(status().isOk());
        put(ADMIN, "/usuarios/" + id, Map.of("username", username, "nombre", "Nuevo", "rol", "VENDEDOR",
                "activo", false)).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders.get("/auth/me").with(httpBasic(username, "secreta1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laRespuestaDeUsuariosNoExponeLaContrasena() throws Exception {
        get(ADMIN, "/usuarios")
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}
