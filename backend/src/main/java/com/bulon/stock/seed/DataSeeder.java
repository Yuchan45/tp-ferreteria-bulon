package com.bulon.stock.seed;

import com.bulon.stock.movimientos.MovimientoRequest;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.CategoriaDTO;
import com.bulon.stock.productos.CategoriaService;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoRequest;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.productos.UnidadMedida;
import com.bulon.stock.proveedores.ProveedorRequest;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.ubicaciones.UbicacionService;
import com.bulon.stock.usuarios.Rol;
import com.bulon.stock.usuarios.Usuario;
import com.bulon.stock.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bulon.stock.productos.UnidadMedida.*;

/**
 * Carga datos de ejemplo de la ferretería al iniciar la aplicación (solo desarrollo).
 * Se activa con {@code app.seed.enabled=true} (variable SEED_ENABLED) y es idempotente:
 * <ul>
 *   <li>Usuarios: crea los que falten y asigna contraseña a los que no tienen.</li>
 *   <li>Catálogo: solo carga si no hay productos.</li>
 * </ul>
 * El stock inicial se registra como movimientos (70 % depósito, 30 % sucursal), así el stock
 * coincide con el historial y las alertas se generan por el mismo camino que en producción.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    public static final String SUCURSAL = "Sucursal";
    public static final String DEPOSITO = "Depósito";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaService categoriaService;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final UbicacionService ubicacionService;
    private final MovimientoStockService movimientoService;

    public record UsuarioSeed(String username, String password, String nombre, Rol rol) {
    }

    private record ProveedorSeed(String clave, String razonSocial, String cuit, String telefono, String email) {
    }

    private record ProductoSeed(String codigo, String nombre, String categoria, UnidadMedida unidad, String precio,
                                int stockMinimo, int stockInicial, String proveedor) {
    }

    public static final List<UsuarioSeed> USUARIOS = List.of(
            new UsuarioSeed("admin", "admin123", "Administrador General", Rol.ADMINISTRADOR),
            new UsuarioSeed("deposito", "deposito123", "Encargado de Depósito", Rol.ENCARGADO_DEPOSITO),
            new UsuarioSeed("ventas", "ventas123", "Vendedor de Mostrador", Rol.VENDEDOR)
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

    // Punto de pedido = 2 × stock mínimo. Algunos quedan bajo el mínimo o en el punto de pedido
    // para probar las alertas de reposición.
    private static final List<ProductoSeed> PRODUCTOS = List.of(
            new ProductoSeed("BUL-001", "Bulón cabeza hexagonal 1/4\" x 2\" zincado", "Bulonería y fijaciones", UNIDAD, "180.00", 200, 850, "BULONERIA"),
            new ProductoSeed("BUL-002", "Bulón cabeza hexagonal 3/8\" x 3\" zincado", "Bulonería y fijaciones", UNIDAD, "345.00", 100, 60, "BULONERIA"),
            new ProductoSeed("TUE-001", "Tuerca hexagonal 1/4\" zincada", "Bulonería y fijaciones", UNIDAD, "35.00", 300, 1200, "BULONERIA"),
            new ProductoSeed("ARA-001", "Arandela plana 1/4\" zincada", "Bulonería y fijaciones", UNIDAD, "15.00", 300, 950, "BULONERIA"),
            new ProductoSeed("TOR-001", "Tornillo autoperforante punta mecha 8 x 1\"", "Bulonería y fijaciones", CAJA, "32.00", 500, 2400, "BULONERIA"),
            new ProductoSeed("HER-001", "Martillo carpintero 27 mm mango de fibra", "Herramientas manuales", UNIDAD, "14500.00", 5, 12, "HERRAMIENTAS"),
            new ProductoSeed("HER-002", "Destornillador Phillips PH2 x 100 mm", "Herramientas manuales", UNIDAD, "4200.00", 10, 25, "HERRAMIENTAS"),
            new ProductoSeed("HER-003", "Pinza universal 8\"", "Herramientas manuales", UNIDAD, "11800.00", 6, 4, "HERRAMIENTAS"),
            new ProductoSeed("HER-004", "Llave ajustable 10\"", "Herramientas manuales", UNIDAD, "16900.00", 5, 9, "HERRAMIENTAS"),
            new ProductoSeed("HER-005", "Cinta métrica 5 m", "Herramientas manuales", UNIDAD, "7600.00", 8, 20, "HERRAMIENTAS"),
            new ProductoSeed("ELE-001", "Cable unipolar 2,5 mm² rollo 100 m", "Electricidad", ROLLO, "52000.00", 4, 7, "ELECTRICIDAD"),
            new ProductoSeed("ELE-002", "Tomacorriente doble 10 A con tierra", "Electricidad", UNIDAD, "3900.00", 15, 40, "ELECTRICIDAD"),
            new ProductoSeed("ELE-003", "Lámpara LED 9 W luz fría E27", "Electricidad", UNIDAD, "2100.00", 30, 18, "ELECTRICIDAD"),
            new ProductoSeed("ELE-004", "Cinta aisladora negra 20 m", "Electricidad", ROLLO, "1250.00", 40, 110, "ELECTRICIDAD"),
            new ProductoSeed("ELE-005", "Interruptor termomagnético bipolar 20 A", "Electricidad", UNIDAD, "18500.00", 4, 6, "ELECTRICIDAD"),
            new ProductoSeed("PLO-001", "Caño termofusión 20 mm x 4 m", "Plomería", UNIDAD, "6800.00", 20, 45, "PLOMERIA"),
            new ProductoSeed("PLO-002", "Codo termofusión 20 mm a 90°", "Plomería", UNIDAD, "450.00", 50, 30, "PLOMERIA"),
            new ProductoSeed("PLO-003", "Llave de paso esférica 1/2\"", "Plomería", UNIDAD, "9300.00", 8, 14, "PLOMERIA"),
            new ProductoSeed("PLO-004", "Cinta de teflón 1/2\" x 10 m", "Plomería", ROLLO, "650.00", 50, 160, "PLOMERIA"),
            new ProductoSeed("PLO-005", "Canilla monocomando para cocina", "Plomería", UNIDAD, "68000.00", 2, 3, "PLOMERIA"),
            new ProductoSeed("PIN-001", "Látex interior blanco mate 20 l", "Pinturería", UNIDAD, "89000.00", 3, 8, "PINTURERIA"),
            new ProductoSeed("PIN-002", "Esmalte sintético blanco brillante 1 l", "Pinturería", LITRO, "14200.00", 6, 15, "PINTURERIA"),
            new ProductoSeed("PIN-003", "Pincel cerda natural 2\"", "Pinturería", UNIDAD, "3100.00", 10, 22, "PINTURERIA"),
            new ProductoSeed("PIN-004", "Rodillo de lana 22 cm", "Pinturería", UNIDAD, "7400.00", 6, 5, "PINTURERIA"),
            new ProductoSeed("PIN-005", "Sellador siliconado transparente 280 ml", "Pinturería", UNIDAD, "5600.00", 12, 0, "PINTURERIA")
    );

    /** Ventas de mostrador de ejemplo para el reporte de rotación. */
    private static final Map<String, Integer> VENTAS = Map.of(
            "BUL-001", 120, "TOR-001", 300, "TUE-001", 200, "ELE-004", 25, "PLO-004", 40, "HER-002", 3);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsuarios();
        if (productoService.listar(null, null, null, false, Pageable.ofSize(1)).isEmpty()) {
            seedCatalogo();
        } else {
            log.info("Seeder: el catálogo ya tiene productos, se omite");
        }
    }

    private void seedUsuarios() {
        for (UsuarioSeed seed : USUARIOS) {
            Usuario usuario = usuarioRepository.findByUsername(seed.username()).orElseGet(() -> {
                Usuario nuevo = new Usuario();
                nuevo.setUsername(seed.username());
                nuevo.setNombre(seed.nombre());
                nuevo.setRol(seed.rol());
                return nuevo;
            });
            if (usuario.getPasswordHash() == null) {
                usuario.setPasswordHash(passwordEncoder.encode(seed.password()));
                usuarioRepository.save(usuario);
                log.info("Seeder: usuario {} listo (contraseña {})", seed.username(), seed.password());
            }
        }
    }

    private void seedCatalogo() {
        Map<String, Long> proveedorIds = new HashMap<>();
        for (ProveedorSeed seed : PROVEEDORES) {
            proveedorIds.put(seed.clave(), proveedorService.crear(
                    new ProveedorRequest(seed.razonSocial(), seed.cuit(), seed.telefono(), seed.email())).id());
        }
        Map<String, Long> categoriaIds = new HashMap<>();
        categoriaService.listar().forEach(c -> categoriaIds.put(c.nombre(), c.id()));

        Long sucursal = ubicacionService.buscarPorNombre(SUCURSAL).getId();
        Long deposito = ubicacionService.buscarPorNombre(DEPOSITO).getId();
        String admin = USUARIOS.getFirst().username();

        for (ProductoSeed seed : PRODUCTOS) {
            Long categoriaId = categoriaIds.computeIfAbsent(seed.categoria(),
                    nombre -> categoriaService.crear(new CategoriaDTO(null, nombre)).id());
            Long proveedorId = proveedorIds.get(seed.proveedor());
            ProductoDTO producto = productoService.crear(new ProductoRequest(seed.codigo(), seed.nombre(), null,
                    categoriaId, seed.unidad(), new BigDecimal(seed.precio()), seed.stockMinimo(),
                    2 * seed.stockMinimo(), true, List.of(proveedorId), proveedorId));

            int enDeposito = Math.round(seed.stockInicial() * 0.7f);
            ingresar(producto.id(), deposito, enDeposito, admin);
            ingresar(producto.id(), sucursal, seed.stockInicial() - enDeposito, admin);

            Integer vendidas = VENTAS.get(seed.codigo());
            if (vendidas != null) {
                movimientoService.registrar(new MovimientoRequest(producto.id(), TipoMovimiento.SALIDA_VENTA,
                        vendidas, sucursal, null, null, "Venta mostrador"), "ventas");
            }
        }
        log.info("Seeder: {} proveedores y {} productos cargados", PROVEEDORES.size(), PRODUCTOS.size());
    }

    private void ingresar(Long productoId, Long ubicacionId, int cantidad, String username) {
        if (cantidad > 0) {
            movimientoService.registrar(new MovimientoRequest(productoId, TipoMovimiento.INGRESO_COMPRA, cantidad,
                    ubicacionId, null, null, "Stock inicial"), username);
        }
    }
}
