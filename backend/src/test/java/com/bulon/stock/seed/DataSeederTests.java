package com.bulon.stock.seed;

import com.bulon.stock.categorias.CategoriaService;
import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoProveedorDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.stock.EstadoStock;
import com.bulon.stock.stock.StockService;
import com.bulon.stock.stock.StockSucursalDTO;
import com.bulon.stock.sucursales.SucursalService;
import com.bulon.stock.usuarios.Rol;
import com.bulon.stock.usuarios.UsuarioDTO;
import com.bulon.stock.usuarios.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.seed.enabled=true",
        // Base H2 propia para no compartir datos con el resto de los tests
        "spring.datasource.url=jdbc:h2:mem:seed_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DataSeederTests {

    @Autowired
    private DataSeeder dataSeeder;
    @Autowired
    private SucursalService sucursalService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private ProveedorService proveedorService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private StockService stockService;
    @Autowired
    private MovimientoStockService movimientoService;

    @Test
    void cargaElCatalogoCompleto() {
        assertThat(sucursalService.listar()).hasSize(1);
        assertThat(usuarioService.listar()).hasSize(3);
        assertThat(categoriaService.listar()).hasSize(5);
        assertThat(proveedorService.listar()).hasSize(6);
        assertThat(productoService.listar()).hasSize(27);
    }

    @Test
    void losEmpleadosPertenecenALaSucursalYElAdministradorNo() {
        Long sucursalId = sucursalService.buscarPorCodigo(DataSeeder.CODIGO_SUCURSAL).orElseThrow().getId();
        for (UsuarioDTO usuario : usuarioService.listar()) {
            assertThat(usuario.sucursalId())
                    .as(usuario.username())
                    .isEqualTo(usuario.rol() == Rol.EMPLEADO ? sucursalId : null);
        }
    }

    @Test
    void dejaProductosAReponerConAmbosNiveles() {
        List<StockSucursalDTO> aReponer = stockService.listarAReponer(null);

        assertThat(aReponer)
                .filteredOn(s -> s.estado() == EstadoStock.CRITICO)
                .extracting(StockSucursalDTO::codigo)
                .containsExactlyInAnyOrder("BUL-002", "HER-003", "ELE-003", "PLO-002", "PIN-004", "PIN-005");
        assertThat(aReponer)
                .filteredOn(s -> s.estado() == EstadoStock.REPONER)
                .extracting(StockSucursalDTO::codigo)
                .containsExactlyInAnyOrder("ELE-005", "PLO-005", "CAD-001");
    }

    @Test
    void cadaProductoTieneUnSoloProveedorPreferido() {
        for (ProductoDTO producto : productoService.listar()) {
            List<ProductoProveedorDTO> proveedores = productoService.listarProveedores(producto.id());
            assertThat(proveedores).as(producto.codigo()).isNotEmpty();
            assertThat(proveedores).filteredOn(ProductoProveedorDTO::preferido).as(producto.codigo()).hasSize(1);
        }
    }

    @Test
    void elStockDeCadaProductoCoincideConSusMovimientos() {
        for (ProductoDTO producto : productoService.listar()) {
            BigDecimal segunMovimientos = movimientoService.listarPorProducto(producto.id(), null).stream()
                    .map(DataSeederTests::efecto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(producto.stockTotal()).as(producto.codigo()).isEqualByComparingTo(segunMovimientos);
        }
    }

    @Test
    void registraUnMovimientoDeCadaTipo() {
        List<TipoMovimiento> tipos = productoService.listar().stream()
                .flatMap(p -> movimientoService.listarPorProducto(p.id(), null).stream())
                .map(MovimientoStockDTO::tipo)
                .distinct()
                .toList();
        assertThat(tipos).containsExactlyInAnyOrder(TipoMovimiento.values());
    }

    @Test
    void admiteCantidadesConDecimales() {
        ProductoDTO clavos = productoService.listar().stream()
                .filter(p -> p.codigo().equals("CLA-001"))
                .findFirst()
                .orElseThrow();
        // 12,75 kg iniciales − 1,25 kg vendidos
        assertThat(clavos.stockTotal()).isEqualByComparingTo("11.5");
    }

    @Test
    void noDuplicaDatosSiSeEjecutaDeNuevo() {
        dataSeeder.run(null);

        assertThat(sucursalService.listar()).hasSize(1);
        assertThat(usuarioService.listar()).hasSize(3);
        assertThat(proveedorService.listar()).hasSize(6);
        assertThat(productoService.listar()).hasSize(27);
    }

    private static BigDecimal efecto(MovimientoStockDTO m) {
        return switch (m.tipo()) {
            case SALIDA_VENTA, DEVOLUCION_PROVEEDOR, ROTURA -> m.cantidad().negate();
            default -> m.cantidad();
        };
    }
}
