package com.bulon.stock.alertas;

import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.notificaciones.NotificacionService;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.stock.StockService;
import com.bulon.stock.stock.UmbralesRequest;
import com.bulon.stock.sucursales.SucursalService;
import com.bulon.stock.usuarios.UsuarioDTO;
import com.bulon.stock.usuarios.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Patrón Observer de punta a punta: un movimiento cambia el stock → StockService avisa a
 * AlertaService → AlertaService avisa a NotificacionService. Sobre los datos del seeder.
 */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:alertas_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AlertasYNotificacionesTests {

    /** El seeder deja 9 productos a reponer y 3 usuarios que operan SUC-01. */
    private static final int A_REPONER_SEEDER = 9;
    private static final int USUARIOS_SUCURSAL = 3;

    @Autowired
    private MovimientoStockService movimientoService;
    @Autowired
    private AlertaService alertaService;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private StockService stockService;
    @Autowired
    private SucursalService sucursalService;
    @Autowired
    private UsuarioService usuarioService;

    private Long sucursalId;
    private Long ventasId;
    private Long adminId;

    @BeforeEach
    void datos() {
        sucursalId = sucursalService.buscarPorCodigo("SUC-01").orElseThrow().getId();
        ventasId = usuario("ventas").id();
        adminId = usuario("admin").id();
    }

    @Test
    void elSeederDejaUnaAlertaPorCadaProductoAReponer() {
        assertThat(alertaService.listarActivas(sucursalId)).hasSize(A_REPONER_SEEDER);
        assertThat(stockService.listarAReponer(sucursalId)).hasSize(A_REPONER_SEEDER);
        assertThat(notificacionService.bandeja(ventasId).noLeidas()).isEqualTo(A_REPONER_SEEDER);
    }

    @Test
    void lasCriticasSeListanPrimero() {
        assertThat(alertaService.listarActivas(sucursalId).getFirst().nivel()).isEqualTo(NivelAlerta.CRITICO);
    }

    @Test
    void unaVentaQueLlegaAlPuntoDePedidoAbreLaAlertaYAvisaATodos() {
        ProductoDTO destornillador = producto("HER-002"); // stock 25, mínimo 10, punto de pedido 15
        long antes = notificacionService.bandeja(adminId).noLeidas();

        registrar(destornillador, TipoMovimiento.SALIDA_VENTA, "10", null); // queda en 15

        AlertaDTO alerta = alertaDe(destornillador).orElseThrow();
        assertThat(alerta.nivel()).isEqualTo(NivelAlerta.REPONER);
        assertThat(alerta.cantidadAlGenerar()).isEqualByComparingTo("15");
        // Le llega al administrador y a los empleados de la sucursal
        assertThat(notificacionService.bandeja(adminId).noLeidas()).isEqualTo(antes + 1);
        assertThat(notificacionService.bandeja(ventasId).notificaciones().getFirst().titulo())
                .isEqualTo("Reponer: " + destornillador.nombre());
    }

    @Test
    void alLlegarAlMinimoLaAlertaSubeACriticoYSeVuelveAAvisar() {
        ProductoDTO destornillador = producto("HER-002");
        registrar(destornillador, TipoMovimiento.SALIDA_VENTA, "10", null); // 15 → REPONER
        long antes = notificacionService.bandeja(ventasId).noLeidas();

        registrar(destornillador, TipoMovimiento.SALIDA_VENTA, "5", null); // 10 → CRITICO

        assertThat(alertaDe(destornillador).orElseThrow().nivel()).isEqualTo(NivelAlerta.CRITICO);
        assertThat(notificacionService.bandeja(ventasId).noLeidas()).isEqualTo(antes + 1);
        assertThat(notificacionService.bandeja(ventasId).notificaciones().getFirst().titulo())
                .startsWith("Stock crítico");
    }

    @Test
    void lasAlertasCriticasTambienSalenPorEmailYLasDeReponerNo(CapturedOutput salida) {
        ProductoDTO destornillador = producto("HER-002");
        // La salida ya trae los emails de las alertas críticas del seeder: se mira solo lo nuevo
        int desde = salida.getOut().length();

        registrar(destornillador, TipoMovimiento.SALIDA_VENTA, "10", null); // REPONER → solo campanita
        assertThat(salida.getOut().substring(desde)).doesNotContain("[EMAIL]");

        registrar(destornillador, TipoMovimiento.SALIDA_VENTA, "5", null);  // CRITICO → campanita + email
        // (solo partes ASCII: la consola de Windows puede no mostrar bien los acentos)
        assertThat(salida.getOut().substring(desde))
                .contains("[EMAIL] a ventas@bulon.com.ar: Stock cr", destornillador.nombre() + " - Quedan 10")
                .contains("[EMAIL] a admin@bulon.com.ar");
    }

    @Test
    void unaCompraQueSuperaElPuntoDePedidoResuelveLaAlertaSinAvisar() {
        ProductoDTO destornillador = producto("HER-002");
        registrar(destornillador, TipoMovimiento.SALIDA_VENTA, "10", null);
        long antes = notificacionService.bandeja(ventasId).noLeidas();

        Long proveedorId = productoService.listarProveedores(destornillador.id()).getFirst().proveedorId();
        registrar(destornillador, TipoMovimiento.INGRESO_COMPRA, "20", proveedorId); // 35

        assertThat(alertaDe(destornillador)).isEmpty();
        assertThat(notificacionService.bandeja(ventasId).noLeidas()).isEqualTo(antes);
    }

    @Test
    void cambiarLosUmbralesTambienAbreLaAlerta() {
        ProductoDTO martillo = producto("HER-001"); // stock 12, punto de pedido 8

        stockService.configurarUmbrales(martillo.id(), sucursalId,
                new UmbralesRequest(new BigDecimal("5"), new BigDecimal("12")));

        assertThat(alertaDe(martillo).orElseThrow().nivel()).isEqualTo(NivelAlerta.REPONER);
    }

    @Test
    void cadaUsuarioSoloMarcaSusNotificaciones() {
        Long deAdmin = notificacionService.bandeja(adminId).notificaciones().getFirst().id();

        assertThatThrownBy(() -> notificacionService.marcarLeida(deAdmin, ventasId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(notificacionService.marcarLeida(deAdmin, adminId).leida()).isTrue();

        notificacionService.marcarTodasLeidas(ventasId);
        assertThat(notificacionService.bandeja(ventasId).noLeidas()).isZero();
        assertThat(notificacionService.bandeja(adminId).noLeidas()).isEqualTo(A_REPONER_SEEDER - 1);
    }

    @Test
    void cadaAlertaNuevaSeAvisaATodosLosUsuariosDeLaSucursal() {
        assertThat(usuarioService.activosDeSucursal(sucursalId)).hasSize(USUARIOS_SUCURSAL);
    }

    private void registrar(ProductoDTO producto, TipoMovimiento tipo, String cantidad, Long proveedorId) {
        movimientoService.registrar(new MovimientoStockDTO(null, producto.id(), sucursalId, ventasId, tipo,
                new BigDecimal(cantidad), null, null, null, null, proveedorId));
    }

    private Optional<AlertaDTO> alertaDe(ProductoDTO producto) {
        return alertaService.listarActivas(sucursalId).stream()
                .filter(a -> a.productoId().equals(producto.id()))
                .findFirst();
    }

    private ProductoDTO producto(String codigo) {
        return productoService.listar().stream().filter(p -> p.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private UsuarioDTO usuario(String username) {
        return usuarioService.listar().stream().filter(u -> u.username().equals(username)).findFirst().orElseThrow();
    }
}
