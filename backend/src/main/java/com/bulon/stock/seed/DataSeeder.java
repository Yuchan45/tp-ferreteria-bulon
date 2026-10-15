package com.bulon.stock.seed;

import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.usuarios.Rol;
import com.bulon.stock.usuarios.Usuario;
import com.bulon.stock.usuarios.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga datos de ejemplo de la ferretería al iniciar la aplicación.
 * Solo se activa con {@code app.seed.enabled=true} (variable SEED_ENABLED) y solo
 * carga en tablas vacías, por lo que es seguro reiniciar la aplicación.
 * El stock inicial se registra como movimientos de ENTRADA para que el stock
 * de cada producto coincida con su historial.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UsuarioService usuarioService;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final MovimientoStockService movimientoService;

    private record UsuarioSeed(String username, String nombre, Rol rol) {
    }

    private record ProveedorSeed(String clave, String razonSocial, String cuit, String telefono, String email) {
    }

    private record ProductoSeed(String codigo, String descripcion, String precio,
                                int stockMinimo, int stockInicial, String proveedor) {
    }

    private static final List<UsuarioSeed> USUARIOS = List.of(
            new UsuarioSeed("admin", "Administrador General", Rol.ADMINISTRADOR),
            new UsuarioSeed("deposito", "Encargado de Depósito", Rol.ENCARGADO_DEPOSITO),
            new UsuarioSeed("ventas", "Vendedor de Mostrador", Rol.VENDEDOR)
    );

    private static final List<ProveedorSeed> PROVEEDORES = List.of(
            new ProveedorSeed("BULONERIA", "Bulonera del Sur S.A.", "30-71000001-1",
                    "011-4201-1000", "ventas@buloneradelsur.com.ar"),
            new ProveedorSeed("HERRAMIENTAS", "Herramientas Patagonia S.R.L.", "30-71000002-2",
                    "0299-442-2000", "pedidos@herramientaspatagonia.com.ar"),
            new ProveedorSeed("ELECTRICIDAD", "Distribuidora Eléctrica Norte S.A.", "30-71000003-3",
                    "0381-421-3000", "comercial@electricanorte.com.ar"),
            new ProveedorSeed("PLOMERIA", "Sanitarios del Litoral S.R.L.", "30-71000004-4",
                    "0342-455-4000", "ventas@sanitarioslitoral.com.ar"),
            new ProveedorSeed("PINTURERIA", "Pinturerías Mayoristas Centro S.A.", "30-71000005-5",
                    "0351-468-5000", "mayorista@pintucentro.com.ar")
    );

    // Algunos productos quedan en o por debajo del stock mínimo para probar las alertas de reposición.
    private static final List<ProductoSeed> PRODUCTOS = List.of(
            // Bulonería y fijaciones
            new ProductoSeed("BUL-001", "Bulón cabeza hexagonal 1/4\" x 2\" zincado", "180.00", 200, 850, "BULONERIA"),
            new ProductoSeed("BUL-002", "Bulón cabeza hexagonal 3/8\" x 3\" zincado", "345.00", 100, 60, "BULONERIA"),
            new ProductoSeed("TUE-001", "Tuerca hexagonal 1/4\" zincada", "35.00", 300, 1200, "BULONERIA"),
            new ProductoSeed("ARA-001", "Arandela plana 1/4\" zincada", "15.00", 300, 950, "BULONERIA"),
            new ProductoSeed("TOR-001", "Tornillo autoperforante punta mecha 8 x 1\"", "32.00", 500, 2400, "BULONERIA"),
            // Herramientas manuales
            new ProductoSeed("HER-001", "Martillo carpintero 27 mm mango de fibra", "14500.00", 5, 12, "HERRAMIENTAS"),
            new ProductoSeed("HER-002", "Destornillador Phillips PH2 x 100 mm", "4200.00", 10, 25, "HERRAMIENTAS"),
            new ProductoSeed("HER-003", "Pinza universal 8\"", "11800.00", 6, 4, "HERRAMIENTAS"),
            new ProductoSeed("HER-004", "Llave ajustable 10\"", "16900.00", 5, 9, "HERRAMIENTAS"),
            new ProductoSeed("HER-005", "Cinta métrica 5 m", "7600.00", 8, 20, "HERRAMIENTAS"),
            // Electricidad
            new ProductoSeed("ELE-001", "Cable unipolar 2,5 mm² rollo 100 m", "52000.00", 4, 7, "ELECTRICIDAD"),
            new ProductoSeed("ELE-002", "Tomacorriente doble 10 A con tierra", "3900.00", 15, 40, "ELECTRICIDAD"),
            new ProductoSeed("ELE-003", "Lámpara LED 9 W luz fría E27", "2100.00", 30, 18, "ELECTRICIDAD"),
            new ProductoSeed("ELE-004", "Cinta aisladora negra 20 m", "1250.00", 40, 110, "ELECTRICIDAD"),
            new ProductoSeed("ELE-005", "Interruptor termomagnético bipolar 20 A", "18500.00", 4, 6, "ELECTRICIDAD"),
            // Plomería
            new ProductoSeed("PLO-001", "Caño termofusión 20 mm x 4 m", "6800.00", 20, 45, "PLOMERIA"),
            new ProductoSeed("PLO-002", "Codo termofusión 20 mm a 90°", "450.00", 50, 30, "PLOMERIA"),
            new ProductoSeed("PLO-003", "Llave de paso esférica 1/2\"", "9300.00", 8, 14, "PLOMERIA"),
            new ProductoSeed("PLO-004", "Cinta de teflón 1/2\" x 10 m", "650.00", 50, 160, "PLOMERIA"),
            new ProductoSeed("PLO-005", "Canilla monocomando para cocina", "68000.00", 2, 3, "PLOMERIA"),
            // Pinturería
            new ProductoSeed("PIN-001", "Látex interior blanco mate 20 l", "89000.00", 3, 8, "PINTURERIA"),
            new ProductoSeed("PIN-002", "Esmalte sintético blanco brillante 1 l", "14200.00", 6, 15, "PINTURERIA"),
            new ProductoSeed("PIN-003", "Pincel cerda natural 2\"", "3100.00", 10, 22, "PINTURERIA"),
            new ProductoSeed("PIN-004", "Rodillo de lana 22 cm", "7400.00", 6, 5, "PINTURERIA"),
            new ProductoSeed("PIN-005", "Sellador siliconado transparente 280 ml", "5600.00", 12, 0, "PINTURERIA")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsuarios();
        seedProveedoresYProductos();
    }

    private void seedUsuarios() {
        if (!usuarioService.listar().isEmpty()) {
            log.info("Seeder: usuarios ya cargados, se omiten");
            return;
        }
        for (UsuarioSeed seed : USUARIOS) {
            Usuario usuario = new Usuario();
            usuario.setUsername(seed.username());
            usuario.setNombre(seed.nombre());
            usuario.setRol(seed.rol());
            usuarioService.crear(usuario);
        }
        log.info("Seeder: {} usuarios cargados", USUARIOS.size());
    }

    private void seedProveedoresYProductos() {
        if (!proveedorService.listar().isEmpty() || !productoService.listar().isEmpty()) {
            log.info("Seeder: proveedores/productos ya cargados, se omiten");
            return;
        }

        Map<String, Long> proveedorIds = new HashMap<>();
        for (ProveedorSeed seed : PROVEEDORES) {
            Proveedor proveedor = new Proveedor();
            proveedor.setRazonSocial(seed.razonSocial());
            proveedor.setCuit(seed.cuit());
            proveedor.setTelefono(seed.telefono());
            proveedor.setEmail(seed.email());
            proveedorIds.put(seed.clave(), proveedorService.crear(proveedor).getId());
        }

        for (ProductoSeed seed : PRODUCTOS) {
            ProductoDTO producto = productoService.crear(new ProductoDTO(
                    null, seed.codigo(), seed.descripcion(), new BigDecimal(seed.precio()),
                    null, seed.stockMinimo(), proveedorIds.get(seed.proveedor())));

            if (seed.stockInicial() > 0) {
                movimientoService.registrar(new MovimientoStockDTO(
                        null, producto.id(), TipoMovimiento.ENTRADA, seed.stockInicial(), null, "Stock inicial"));
            }
        }
        log.info("Seeder: {} proveedores y {} productos cargados", PROVEEDORES.size(), PRODUCTOS.size());
    }
}
