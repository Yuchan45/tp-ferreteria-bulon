package com.bulon.stock.seed;

import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.usuarios.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
    private UsuarioService usuarioService;
    @Autowired
    private ProveedorService proveedorService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private MovimientoStockService movimientoService;

    @Test
    void cargaUsuariosProveedoresYProductos() {
        assertThat(usuarioService.listar()).hasSize(3);
        assertThat(proveedorService.listar()).hasSize(5);
        assertThat(productoService.listar()).hasSize(25);
    }

    @Test
    void dejaProductosBajoStockMinimoParaLasAlertas() {
        assertThat(productoService.listarBajoStockMinimo())
                .extracting(ProductoDTO::codigo)
                .containsExactlyInAnyOrder("BUL-002", "HER-003", "ELE-003", "PLO-002", "PIN-004", "PIN-005");
    }

    @Test
    void elStockDeCadaProductoCoincideConSusMovimientos() {
        for (ProductoDTO producto : productoService.listar()) {
            List<MovimientoStockDTO> movimientos = movimientoService.listarPorProducto(producto.id());
            int stockSegunMovimientos = movimientos.stream()
                    .mapToInt(m -> m.tipo() == TipoMovimiento.SALIDA ? -m.cantidad() : m.cantidad())
                    .sum();
            assertThat(producto.stockActual()).as(producto.codigo()).isEqualTo(stockSegunMovimientos);
        }
    }

    @Test
    void noDuplicaDatosSiSeEjecutaDeNuevo() {
        dataSeeder.run(null);

        assertThat(usuarioService.listar()).hasSize(3);
        assertThat(proveedorService.listar()).hasSize(5);
        assertThat(productoService.listar()).hasSize(25);
    }
}
