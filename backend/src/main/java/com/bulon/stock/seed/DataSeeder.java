package com.bulon.stock.seed;

import com.bulon.stock.categorias.Categoria;
import com.bulon.stock.categorias.CategoriaService;
import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoProveedorDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.productos.UnidadMedida;
import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.proveedores.ProveedorService;
import com.bulon.stock.stock.StockService;
import com.bulon.stock.stock.UmbralesRequest;
import com.bulon.stock.sucursales.Sucursal;
import com.bulon.stock.sucursales.SucursalService;
import com.bulon.stock.usuarios.Rol;
import com.bulon.stock.usuarios.UsuarioDTO;
import com.bulon.stock.usuarios.UsuarioRequest;
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
 * Carga datos de ejemplo de la ferretería al iniciar la aplicación (modelo v2).
 * Solo se activa con {@code app.seed.enabled=true} (variable SEED_ENABLED) y solo
 * carga en tablas vacías, por lo que es seguro reiniciar la aplicación.
 * <p>
 * El stock inicial se registra como compras (INGRESO_COMPRA) al proveedor preferido,
 * así el stock de cada sucursal coincide con su historial. Algunos productos quedan
 * en o por debajo del punto de pedido para probar las alertas de reposición.
 * Contraseñas de desarrollo: {@value #PASSWORD_ADMIN} para el administrador y
 * {@value #PASSWORD_EMPLEADO} para los empleados.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    // "admin" es más corta que el mínimo de 8 que exige la API: el seeder crea los usuarios
    // con el servicio, que no pasa por @Valid. Desde la app no se podría poner.
    static final String PASSWORD_ADMIN = "admin";
    static final String PASSWORD_EMPLEADO = "empleado";
    static final String CODIGO_SUCURSAL = "SUC-01";

    private final SucursalService sucursalService;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final StockService stockService;
    private final MovimientoStockService movimientoService;

    private record UsuarioSeed(String username, String nombre, String email, Rol rol) {
    }

    private record ProveedorSeed(String clave, String razonSocial, String cuit, String telefono,
                                 String email, String contacto) {
    }

    /** Proveedor preferido del producto: costo y plazo de entrega. */
    private record ProductoSeed(String codigo, String nombre, String descripcion, String categoria,
                                UnidadMedida unidad, String precio, String stockMinimo, String puntoPedido,
                                String stockInicial, String proveedor, String costo, int plazoDias) {
    }

    /** Segundo proveedor (no preferido) de un producto, para comparar condiciones. */
    private record AlternativoSeed(String codigo, String proveedor, String costo, int plazoDias) {
    }

    private record MovimientoSeed(String codigo, TipoMovimiento tipo, String cantidad, String motivo,
                                  String referencia, String proveedor) {
    }

    private static final List<String> CATEGORIAS = List.of(
            "Bulonería y fijaciones", "Herramientas manuales", "Electricidad", "Plomería", "Pinturería");

    private static final List<UsuarioSeed> USUARIOS = List.of(
            new UsuarioSeed("admin", "Administrador General", "admin@bulon.com.ar", Rol.ADMINISTRADOR),
            new UsuarioSeed("encargado", "Encargado de Sucursal", "encargado@bulon.com.ar", Rol.EMPLEADO),
            new UsuarioSeed("ventas", "Vendedor de Mostrador", "ventas@bulon.com.ar", Rol.EMPLEADO)
    );

    private static final List<ProveedorSeed> PROVEEDORES = List.of(
            new ProveedorSeed("BULONERIA", "Bulonera del Sur S.A.", "30-71000001-1",
                    "011-4201-1000", "ventas@buloneradelsur.com.ar", "Marta Gómez"),
            new ProveedorSeed("HERRAMIENTAS", "Herramientas Patagonia S.R.L.", "30-71000002-2",
                    "0299-442-2000", "pedidos@herramientaspatagonia.com.ar", "Julián Ríos"),
            new ProveedorSeed("ELECTRICIDAD", "Distribuidora Eléctrica Norte S.A.", "30-71000003-3",
                    "0381-421-3000", "comercial@electricanorte.com.ar", "Carla Medina"),
            new ProveedorSeed("PLOMERIA", "Sanitarios del Litoral S.R.L.", "30-71000004-4",
                    "0342-455-4000", "ventas@sanitarioslitoral.com.ar", "Diego Paz"),
            new ProveedorSeed("PINTURERIA", "Pinturerías Mayoristas Centro S.A.", "30-71000005-5",
                    "0351-468-5000", "mayorista@pintucentro.com.ar", "Lucía Ferreyra"),
            // Mayorista general: vende productos de varias familias con otras condiciones
            new ProveedorSeed("MAYORISTA", "Ferretería Mayorista Oeste S.A.", "30-71000006-6",
                    "011-4655-6000", "pedidos@mayoristaoeste.com.ar", "Pablo Sosa")
    );

    // stockMinimo < puntoPedido. Quedan en o por debajo del punto de pedido:
    // CRITICO (≤ mínimo): BUL-002, HER-003, ELE-003, PLO-002, PIN-004, PIN-005
    // REPONER (entre mínimo y punto de pedido): ELE-005, PLO-005, CAD-001
    private static final List<ProductoSeed> PRODUCTOS = List.of(
            // Bulonería y fijaciones
            new ProductoSeed("BUL-001", "Bulón hexagonal 1/4\" x 2\"", "Bulón cabeza hexagonal 1/4\" x 2\" zincado",
                    "Bulonería y fijaciones", UnidadMedida.UNIDAD, "180.00", "200", "300", "850", "BULONERIA", "110.00", 3),
            new ProductoSeed("BUL-002", "Bulón hexagonal 3/8\" x 3\"", "Bulón cabeza hexagonal 3/8\" x 3\" zincado",
                    "Bulonería y fijaciones", UnidadMedida.UNIDAD, "345.00", "100", "150", "60", "BULONERIA", "210.00", 3),
            new ProductoSeed("TUE-001", "Tuerca hexagonal 1/4\"", "Tuerca hexagonal 1/4\" zincada",
                    "Bulonería y fijaciones", UnidadMedida.UNIDAD, "35.00", "300", "450", "1200", "BULONERIA", "20.00", 3),
            new ProductoSeed("ARA-001", "Arandela plana 1/4\"", "Arandela plana 1/4\" zincada",
                    "Bulonería y fijaciones", UnidadMedida.UNIDAD, "15.00", "300", "450", "950", "BULONERIA", "8.00", 3),
            new ProductoSeed("TOR-001", "Tornillo autoperforante 8 x 1\"", "Tornillo autoperforante punta mecha 8 x 1\"",
                    "Bulonería y fijaciones", UnidadMedida.UNIDAD, "32.00", "500", "750", "2400", "BULONERIA", "18.00", 3),
            new ProductoSeed("CAD-001", "Cadena galvanizada 6 mm", "Cadena eslabón corto galvanizada 6 mm, por metro",
                    "Bulonería y fijaciones", UnidadMedida.METRO, "2900.00", "20", "30", "25.5", "BULONERIA", "1750.00", 5),
            new ProductoSeed("CLA-001", "Clavos punta París 2\"", "Clavos punta París 2\" sueltos, por kilo",
                    "Bulonería y fijaciones", UnidadMedida.KILOGRAMO, "6500.00", "5", "8", "12.75", "BULONERIA", "3900.00", 3),
            // Herramientas manuales
            new ProductoSeed("HER-001", "Martillo carpintero 27 mm", "Martillo carpintero 27 mm mango de fibra",
                    "Herramientas manuales", UnidadMedida.UNIDAD, "14500.00", "5", "8", "12", "HERRAMIENTAS", "9200.00", 7),
            new ProductoSeed("HER-002", "Destornillador Phillips PH2", "Destornillador Phillips PH2 x 100 mm",
                    "Herramientas manuales", UnidadMedida.UNIDAD, "4200.00", "10", "15", "25", "HERRAMIENTAS", "2600.00", 7),
            new ProductoSeed("HER-003", "Pinza universal 8\"", "Pinza universal 8\" mango aislado",
                    "Herramientas manuales", UnidadMedida.UNIDAD, "11800.00", "6", "9", "4", "HERRAMIENTAS", "7400.00", 7),
            new ProductoSeed("HER-004", "Llave ajustable 10\"", "Llave ajustable 10\" cromada",
                    "Herramientas manuales", UnidadMedida.UNIDAD, "16900.00", "5", "8", "9", "HERRAMIENTAS", "10500.00", 7),
            new ProductoSeed("HER-005", "Cinta métrica 5 m", "Cinta métrica 5 m con freno",
                    "Herramientas manuales", UnidadMedida.UNIDAD, "7600.00", "8", "12", "20", "HERRAMIENTAS", "4700.00", 7),
            // Electricidad
            new ProductoSeed("ELE-001", "Cable unipolar 2,5 mm²", "Cable unipolar 2,5 mm² rollo 100 m",
                    "Electricidad", UnidadMedida.ROLLO, "52000.00", "4", "6", "7", "ELECTRICIDAD", "34000.00", 5),
            new ProductoSeed("ELE-002", "Tomacorriente doble 10 A", "Tomacorriente doble 10 A con tierra",
                    "Electricidad", UnidadMedida.UNIDAD, "3900.00", "15", "25", "40", "ELECTRICIDAD", "2300.00", 5),
            new ProductoSeed("ELE-003", "Lámpara LED 9 W", "Lámpara LED 9 W luz fría E27",
                    "Electricidad", UnidadMedida.UNIDAD, "2100.00", "30", "45", "18", "ELECTRICIDAD", "1200.00", 5),
            new ProductoSeed("ELE-004", "Cinta aisladora 20 m", "Cinta aisladora negra 20 m",
                    "Electricidad", UnidadMedida.UNIDAD, "1250.00", "40", "60", "110", "ELECTRICIDAD", "700.00", 5),
            new ProductoSeed("ELE-005", "Termomagnética bipolar 20 A", "Interruptor termomagnético bipolar 20 A",
                    "Electricidad", UnidadMedida.UNIDAD, "18500.00", "4", "6", "6", "ELECTRICIDAD", "12000.00", 5),
            // Plomería
            new ProductoSeed("PLO-001", "Caño termofusión 20 mm", "Caño termofusión 20 mm x 4 m",
                    "Plomería", UnidadMedida.UNIDAD, "6800.00", "20", "30", "45", "PLOMERIA", "4100.00", 4),
            new ProductoSeed("PLO-002", "Codo termofusión 20 mm 90°", "Codo termofusión 20 mm a 90°",
                    "Plomería", UnidadMedida.UNIDAD, "450.00", "50", "75", "30", "PLOMERIA", "250.00", 4),
            new ProductoSeed("PLO-003", "Llave de paso 1/2\"", "Llave de paso esférica 1/2\"",
                    "Plomería", UnidadMedida.UNIDAD, "9300.00", "8", "12", "14", "PLOMERIA", "5800.00", 4),
            new ProductoSeed("PLO-004", "Cinta de teflón 1/2\"", "Cinta de teflón 1/2\" x 10 m",
                    "Plomería", UnidadMedida.UNIDAD, "650.00", "50", "75", "160", "PLOMERIA", "350.00", 4),
            new ProductoSeed("PLO-005", "Canilla monocomando cocina", "Canilla monocomando para cocina",
                    "Plomería", UnidadMedida.UNIDAD, "68000.00", "2", "3", "3", "PLOMERIA", "45000.00", 10),
            // Pinturería
            new ProductoSeed("PIN-001", "Látex interior 20 l", "Látex interior blanco mate 20 l",
                    "Pinturería", UnidadMedida.UNIDAD, "89000.00", "3", "5", "8", "PINTURERIA", "61000.00", 6),
            new ProductoSeed("PIN-002", "Esmalte sintético 1 l", "Esmalte sintético blanco brillante 1 l",
                    "Pinturería", UnidadMedida.UNIDAD, "14200.00", "6", "9", "15", "PINTURERIA", "9100.00", 6),
            new ProductoSeed("PIN-003", "Pincel cerda natural 2\"", "Pincel cerda natural 2\"",
                    "Pinturería", UnidadMedida.UNIDAD, "3100.00", "10", "15", "22", "PINTURERIA", "1800.00", 6),
            new ProductoSeed("PIN-004", "Rodillo de lana 22 cm", "Rodillo de lana 22 cm con mango",
                    "Pinturería", UnidadMedida.UNIDAD, "7400.00", "6", "9", "5", "PINTURERIA", "4500.00", 6),
            new ProductoSeed("PIN-005", "Sellador siliconado 280 ml", "Sellador siliconado transparente 280 ml",
                    "Pinturería", UnidadMedida.UNIDAD, "5600.00", "12", "18", "0", "PINTURERIA", "3300.00", 6)
    );

    // El mayorista es más barato en algunos productos y más rápido en otros
    private static final List<AlternativoSeed> ALTERNATIVOS = List.of(
            new AlternativoSeed("BUL-002", "MAYORISTA", "198.00", 5),
            new AlternativoSeed("TOR-001", "MAYORISTA", "17.00", 2),
            new AlternativoSeed("HER-003", "MAYORISTA", "7900.00", 2),
            new AlternativoSeed("ELE-003", "MAYORISTA", "1150.00", 8),
            new AlternativoSeed("PLO-002", "MAYORISTA", "240.00", 3),
            new AlternativoSeed("PIN-005", "MAYORISTA", "3450.00", 1)
    );

    // Un ejemplo de cada tipo restante, sobre productos con stock holgado (no cambian su estado)
    private static final List<MovimientoSeed> MOVIMIENTOS = List.of(
            new MovimientoSeed("BUL-001", TipoMovimiento.SALIDA_VENTA, "50", null, "TK-0001-00000101", null),
            new MovimientoSeed("CLA-001", TipoMovimiento.SALIDA_VENTA, "1.25", null, "TK-0001-00000102", null),
            new MovimientoSeed("BUL-001", TipoMovimiento.DEVOLUCION_CLIENTE, "5",
                    "Cliente devolvió excedente de obra", "TK-0001-00000101", null),
            new MovimientoSeed("TOR-001", TipoMovimiento.ROTURA, "20", "Caja dañada en góndola", null, null),
            new MovimientoSeed("TUE-001", TipoMovimiento.AJUSTE, "-15", "Recuento: faltante", null, null),
            new MovimientoSeed("ARA-001", TipoMovimiento.DEVOLUCION_PROVEEDOR, "50",
                    "Lote con fallas de zincado", "NC-0003-00000021", "BULONERIA")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Sucursal sucursal = seedSucursal();
        seedUsuarios(sucursal);
        seedCatalogo(sucursal);
    }

    /** La migración V2 ya crea SUC-01; en una base creada por Hibernate (tests) hay que crearla. */
    private Sucursal seedSucursal() {
        return sucursalService.buscarPorCodigo(CODIGO_SUCURSAL).orElseGet(() -> {
            Sucursal sucursal = new Sucursal();
            sucursal.setCodigo(CODIGO_SUCURSAL);
            sucursal.setNombre("Sucursal Central");
            log.info("Seeder: sucursal {} creada", CODIGO_SUCURSAL);
            return sucursalService.crear(sucursal);
        });
    }

    private void seedUsuarios(Sucursal sucursal) {
        if (!usuarioService.listar().isEmpty()) {
            log.info("Seeder: usuarios ya cargados, se omiten");
            return;
        }
        for (UsuarioSeed seed : USUARIOS) {
            boolean admin = seed.rol() == Rol.ADMINISTRADOR;
            usuarioService.crear(new UsuarioRequest(seed.username(), seed.nombre(), seed.email(),
                    admin ? PASSWORD_ADMIN : PASSWORD_EMPLEADO, seed.rol(), admin ? null : sucursal.getId(), true));
        }
        log.info("Seeder: {} usuarios cargados (contraseñas de desarrollo: admin → {}, empleados → {})",
                USUARIOS.size(), PASSWORD_ADMIN, PASSWORD_EMPLEADO);
    }

    private void seedCatalogo(Sucursal sucursal) {
        if (!proveedorService.listar().isEmpty() || !productoService.listar().isEmpty()) {
            log.info("Seeder: proveedores/productos ya cargados, se omiten");
            return;
        }

        Map<String, Long> categoriaIds = new HashMap<>();
        for (String nombre : CATEGORIAS) {
            Categoria categoria = new Categoria();
            categoria.setNombre(nombre);
            categoriaIds.put(nombre, categoriaService.crear(categoria).getId());
        }

        Map<String, Long> proveedorIds = new HashMap<>();
        for (ProveedorSeed seed : PROVEEDORES) {
            Proveedor proveedor = new Proveedor();
            proveedor.setRazonSocial(seed.razonSocial());
            proveedor.setCuit(seed.cuit());
            proveedor.setTelefono(seed.telefono());
            proveedor.setEmail(seed.email());
            proveedor.setContacto(seed.contacto());
            proveedorIds.put(seed.clave(), proveedorService.crear(proveedor).getId());
        }

        // Los movimientos de ejemplo los registra el encargado de la sucursal
        Long responsableId = usuarioService.listar().stream()
                .filter(u -> u.rol() == Rol.EMPLEADO)
                .map(UsuarioDTO::id)
                .findFirst()
                .orElseThrow();

        Map<String, Long> productoIds = new HashMap<>();
        int remito = 1;
        for (ProductoSeed seed : PRODUCTOS) {
            ProductoDTO producto = productoService.crear(new ProductoDTO(
                    null, seed.codigo(), seed.nombre(), seed.descripcion(), categoriaIds.get(seed.categoria()),
                    seed.unidad(), new BigDecimal(seed.precio()), true, null));
            productoIds.put(seed.codigo(), producto.id());

            Long proveedorId = proveedorIds.get(seed.proveedor());
            productoService.asignarProveedor(producto.id(), proveedorId, new ProductoProveedorDTO(
                    proveedorId, null, true, "P-" + seed.codigo(), new BigDecimal(seed.costo()), seed.plazoDias()));
            stockService.configurarUmbrales(producto.id(), sucursal.getId(), new UmbralesRequest(
                    new BigDecimal(seed.stockMinimo()), new BigDecimal(seed.puntoPedido())));

            BigDecimal inicial = new BigDecimal(seed.stockInicial());
            if (inicial.signum() > 0) {
                movimientoService.registrar(new MovimientoStockDTO(
                        null, producto.id(), sucursal.getId(), responsableId, TipoMovimiento.INGRESO_COMPRA, inicial,
                        null, null, "Stock inicial", String.format("REM-0001-%08d", remito++), proveedorId));
            }
        }

        for (AlternativoSeed seed : ALTERNATIVOS) {
            Long proveedorId = proveedorIds.get(seed.proveedor());
            productoService.asignarProveedor(productoIds.get(seed.codigo()), proveedorId, new ProductoProveedorDTO(
                    proveedorId, null, false, "MO-" + seed.codigo(), new BigDecimal(seed.costo()), seed.plazoDias()));
        }

        for (MovimientoSeed seed : MOVIMIENTOS) {
            movimientoService.registrar(new MovimientoStockDTO(
                    null, productoIds.get(seed.codigo()), sucursal.getId(), responsableId, seed.tipo(),
                    new BigDecimal(seed.cantidad()), null, seed.motivo(), null, seed.referencia(),
                    seed.proveedor() != null ? proveedorIds.get(seed.proveedor()) : null));
        }

        log.info("Seeder: {} categorías, {} proveedores, {} productos y {} movimientos de ejemplo cargados",
                CATEGORIAS.size(), PROVEEDORES.size(), PRODUCTOS.size(), remito - 1 + MOVIMIENTOS.size());
    }
}
