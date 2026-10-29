package com.bulon.stock.movimientos;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.stock.StockService;
import com.bulon.stock.sucursales.Sucursal;
import com.bulon.stock.sucursales.SucursalService;
import com.bulon.stock.usuarios.UsuarioDTO;
import com.bulon.stock.usuarios.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de registro de movimientos, sobre los datos del seeder. Cada test se revierte. */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:movimientos_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
@Transactional
class MovimientoStockServiceTests {

    @Autowired
    private MovimientoStockService movimientoService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private ProveedorService proveedorService;
    @Autowired
    private SucursalService sucursalService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private StockService stockService;

    private Long sucursalId;
    private Long vendedorId;
    private Long adminId;

    @BeforeEach
    void datos() {
        sucursalId = sucursalService.buscarPorCodigo("SUC-01").orElseThrow().getId();
        vendedorId = usuario("ventas").id();
        adminId = usuario("admin").id();
    }

    @Test
    void unaVentaDescuentaElStockDeLaSucursal() {
        ProductoDTO tornillo = producto("TOR-001");

        movimientoService.registrar(movimiento(tornillo.id(), vendedorId, TipoMovimiento.SALIDA_VENTA, "100", null, null));

        assertThat(productoService.obtener(tornillo.id()).stockTotal())
                .isEqualByComparingTo(tornillo.stockTotal().subtract(new BigDecimal("100")));
    }

    @Test
    void rechazaUnaSalidaMayorAlStock() {
        ProductoDTO pinza = producto("HER-003"); // stock 4

        assertThatThrownBy(() -> movimientoService.registrar(
                movimiento(pinza.id(), vendedorId, TipoMovimiento.SALIDA_VENTA, "10", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void elEmpleadoSoloOperaSuSucursalYElAdministradorTodas() {
        Sucursal otra = new Sucursal();
        otra.setCodigo("SUC-02");
        otra.setNombre("Sucursal Norte");
        Long otraId = sucursalService.crear(otra).getId();
        ProductoDTO tornillo = producto("TOR-001");
        stockService.crearFilasPara(productoService.buscarEntidad(tornillo.id()));
        Long proveedorId = productoService.listarProveedores(tornillo.id()).getFirst().proveedorId();

        MovimientoStockDTO compraEmpleado = new MovimientoStockDTO(null, tornillo.id(), otraId, vendedorId,
                TipoMovimiento.INGRESO_COMPRA, new BigDecimal("10"), null, null, null, null, proveedorId);
        assertThatThrownBy(() -> movimientoService.registrar(compraEmpleado))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solo puede registrar movimientos de su sucursal");

        MovimientoStockDTO compraAdmin = new MovimientoStockDTO(null, tornillo.id(), otraId, adminId,
                TipoMovimiento.INGRESO_COMPRA, new BigDecimal("10"), null, null, null, null, proveedorId);
        assertThat(movimientoService.registrar(compraAdmin).sucursalId()).isEqualTo(otraId);
    }

    @Test
    void laCompraExigeUnProveedorQueVendaElProducto() {
        ProductoDTO pinza = producto("HER-003");
        Long sanitarios = proveedorService.listar().stream()
                .filter(p -> p.getRazonSocial().startsWith("Sanitarios"))
                .map(Proveedor::getId)
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> movimientoService.registrar(
                movimiento(pinza.id(), vendedorId, TipoMovimiento.INGRESO_COMPRA, "5", null, null)))
                .hasMessageContaining("El proveedor es obligatorio");
        assertThatThrownBy(() -> movimientoService.registrar(
                movimiento(pinza.id(), vendedorId, TipoMovimiento.INGRESO_COMPRA, "5", null, sanitarios)))
                .hasMessageContaining("no vende el producto");
    }

    @Test
    void soloLosProductosFraccionablesAdmitenDecimales() {
        ProductoDTO martillo = producto("HER-001"); // por unidad
        ProductoDTO cadena = producto("CAD-001");   // por metro

        assertThatThrownBy(() -> movimientoService.registrar(
                movimiento(martillo.id(), vendedorId, TipoMovimiento.SALIDA_VENTA, "1.5", null, null)))
                .hasMessageContaining("la cantidad debe ser entera");
        assertThat(movimientoService.registrar(
                movimiento(cadena.id(), vendedorId, TipoMovimiento.SALIDA_VENTA, "1.5", null, null)).cantidad())
                .isEqualByComparingTo("1.5");
    }

    @Test
    void elAjusteExigeMotivoYNoPuedeSerCero() {
        ProductoDTO tuerca = producto("TUE-001");

        assertThatThrownBy(() -> movimientoService.registrar(
                movimiento(tuerca.id(), vendedorId, TipoMovimiento.AJUSTE, "-3", null, null)))
                .hasMessageContaining("El motivo es obligatorio");
        assertThatThrownBy(() -> movimientoService.registrar(
                movimiento(tuerca.id(), vendedorId, TipoMovimiento.AJUSTE, "0", "Recuento", null)))
                .hasMessageContaining("no puede ser 0");
    }

    private MovimientoStockDTO movimiento(Long productoId, Long usuarioId, TipoMovimiento tipo, String cantidad,
                                          String motivo, Long proveedorId) {
        return new MovimientoStockDTO(null, productoId, sucursalId, usuarioId, tipo, new BigDecimal(cantidad),
                null, motivo, null, null, proveedorId);
    }

    private ProductoDTO producto(String codigo) {
        return productoService.listar().stream().filter(p -> p.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private UsuarioDTO usuario(String username) {
        return usuarioService.listar().stream().filter(u -> u.username().equals(username)).findFirst().orElseThrow();
    }
}
