package com.bulon.stock.reportes;

import com.bulon.stock.categorias.Categoria;
import com.bulon.stock.categorias.CategoriaService;
import com.bulon.stock.common.Busqueda;
import com.bulon.stock.movimientos.FiltroMovimientos;
import com.bulon.stock.movimientos.MovimientoDetalleDTO;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.ProductoProveedorDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.reportes.Reporte.Columna;
import com.bulon.stock.stock.EstadoStock;
import com.bulon.stock.stock.StockService;
import com.bulon.stock.stock.StockSucursalDTO;
import com.bulon.stock.sucursales.Sucursal;
import com.bulon.stock.sucursales.SucursalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Arma los reportes con los datos de los otros módulos y los exporta con el
 * {@link ExportadorReporte} del formato pedido. No conoce OpenPDF ni Apache POI:
 * eso queda en los adaptadores (patrón Adapter).
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<EstadoStock, String> ESTADO = Map.of(
            EstadoStock.OK, "Normal", EstadoStock.REPONER, "A reponer", EstadoStock.CRITICO, "Crítico");

    private static final Map<TipoMovimiento, String> TIPO = Map.of(
            TipoMovimiento.INGRESO_COMPRA, "Compra",
            TipoMovimiento.SALIDA_VENTA, "Venta",
            TipoMovimiento.DEVOLUCION_CLIENTE, "Devolución de cliente",
            TipoMovimiento.DEVOLUCION_PROVEEDOR, "Devolución a proveedor",
            TipoMovimiento.ROTURA, "Rotura",
            TipoMovimiento.AJUSTE, "Ajuste");

    private final StockService stockService;
    private final MovimientoStockService movimientoService;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final CategoriaService categoriaService;
    /** Un exportador por formato: Spring inyecta todos los adaptadores. */
    private final Map<FormatoReporte, ExportadorReporte> exportadores = new EnumMap<>(FormatoReporte.class);

    public ReporteService(StockService stockService, MovimientoStockService movimientoService,
                          ProductoService productoService, SucursalService sucursalService,
                          CategoriaService categoriaService, List<ExportadorReporte> exportadores) {
        this.stockService = stockService;
        this.movimientoService = movimientoService;
        this.productoService = productoService;
        this.sucursalService = sucursalService;
        this.categoriaService = categoriaService;
        exportadores.forEach(e -> this.exportadores.put(e.formato(), e));
    }

    /** El archivo listo para descargar. */
    public record ArchivoReporte(String nombre, String contentType, byte[] contenido) {
    }

    public ArchivoReporte stock(Long sucursalId, Long categoriaId, EstadoStock estado, String busqueda,
                                FormatoReporte formato) {
        Sucursal sucursal = sucursalService.obtener(sucursalId);
        Map<Long, String> categorias = categoriaService.listar().stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));
        Map<Long, Long> categoriaDe = new HashMap<>();
        productoService.listar().forEach(p -> categoriaDe.put(p.id(), p.categoriaId()));

        List<StockSucursalDTO> lista = stockService.listarPorSucursal(sucursalId).stream()
                .filter(s -> categoriaId == null || categoriaId.equals(categoriaDe.get(s.productoId())))
                .filter(s -> estado == null || s.estado() == estado)
                .filter(s -> Busqueda.coincide(busqueda, s.codigo(), s.nombre()))
                .sorted(Comparator.comparing(StockSucursalDTO::codigo))
                .toList();

        List<List<Object>> filas = lista.stream().<List<Object>>map(s -> Arrays.asList(
                s.codigo(), s.nombre(), categorias.getOrDefault(categoriaDe.get(s.productoId()), ""),
                s.cantidad(), s.stockMinimo(), s.puntoPedido(), ESTADO.get(s.estado())
        )).toList();

        List<String> filtros = new ArrayList<>();
        if (categoriaId != null) {
            filtros.add("Categoría: " + categorias.getOrDefault(categoriaId, "—"));
        }
        filtros.addAll(filtros(estado, busqueda));

        Reporte reporte = new Reporte("Stock por sucursal",
                detalles(sucursal, filtros),
                List.of(Columna.texto("Código"), Columna.texto("Producto"), Columna.texto("Categoría"),
                        Columna.numero("Stock"), Columna.numero("Mínimo"), Columna.numero("Pto. pedido"),
                        Columna.texto("Estado")),
                filas,
                lista.size() + " productos · " + contar(lista, EstadoStock.CRITICO) + " críticos · "
                        + contar(lista, EstadoStock.REPONER) + " a reponer");
        return exportar(reporte, "stock", sucursal, formato);
    }

    public ArchivoReporte movimientos(Long sucursalId, LocalDate desde, LocalDate hasta, TipoMovimiento tipo,
                                      String busqueda, FormatoReporte formato) {
        Sucursal sucursal = sucursalService.obtener(sucursalId);
        // La misma consulta que la vista Movimientos (GET /movimientos), con sus mismos filtros
        List<MovimientoDetalleDTO> lista = movimientoService.listar(
                new FiltroMovimientos(null, sucursalId, desde, hasta, tipo, busqueda));

        List<List<Object>> filas = new ArrayList<>();
        int ingresos = 0;
        for (MovimientoDetalleDTO m : lista) {
            if (m.efecto().signum() > 0) {
                ingresos++;
            }
            filas.add(Arrays.asList(
                    m.fecha().format(FECHA_HORA), m.codigo(), m.nombre(), TIPO.get(m.tipo()), m.efecto(),
                    m.stockResultante(), m.usuario(), m.proveedor() != null ? m.proveedor() : "", detalle(m)));
        }

        List<String> filtros = new ArrayList<>();
        filtros.add("Período: " + (desde != null ? desde.format(FECHA) : "inicio") + " al "
                + (hasta != null ? hasta.format(FECHA) : "hoy"));
        if (tipo != null) {
            filtros.add("Tipo: " + TIPO.get(tipo));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            filtros.add("Producto: \"" + busqueda.trim() + "\"");
        }

        Reporte reporte = new Reporte("Movimientos por período",
                detalles(sucursal, filtros),
                List.of(Columna.texto("Fecha"), Columna.texto("Código"), Columna.texto("Producto"),
                        Columna.texto("Tipo"), Columna.numero("Cantidad"), Columna.numero("Stock resultante"),
                        Columna.texto("Responsable"),
                        Columna.texto("Proveedor"), Columna.texto("Detalle")),
                filas,
                lista.size() + " movimientos · " + ingresos + " ingresos · " + (lista.size() - ingresos) + " salidas");
        return exportar(reporte, "movimientos", sucursal, formato);
    }

    public ArchivoReporte aReponer(Long sucursalId, EstadoStock estado, String busqueda, FormatoReporte formato) {
        Sucursal sucursal = sucursalService.obtener(sucursalId);
        // Igual que la vista: primero los críticos y después los que más faltan
        List<StockSucursalDTO> lista = stockService.listarAReponer(sucursalId).stream()
                .filter(s -> estado == null || s.estado() == estado)
                .filter(s -> Busqueda.coincide(busqueda, s.codigo(), s.nombre()))
                .sorted(Comparator.comparing((StockSucursalDTO s) -> s.estado() != EstadoStock.CRITICO)
                        .thenComparing(ReporteService::faltante, Comparator.reverseOrder()))
                .toList();

        List<List<Object>> filas = new ArrayList<>();
        BigDecimal costoTotal = BigDecimal.ZERO;
        for (StockSucursalDTO s : lista) {
            Optional<ProductoProveedorDTO> preferido = productoService.listarProveedores(s.productoId()).stream()
                    .filter(ProductoProveedorDTO::preferido)
                    .findFirst();
            BigDecimal costo = preferido.map(ProductoProveedorDTO::precioCosto).orElse(null);
            BigDecimal estimado = costo != null ? costo.multiply(faltante(s)) : null;
            if (estimado != null) {
                costoTotal = costoTotal.add(estimado);
            }
            filas.add(Arrays.asList(
                    s.codigo(), s.nombre(), s.cantidad(), s.stockMinimo(), s.puntoPedido(), faltante(s),
                    ESTADO.get(s.estado()), preferido.map(ProductoProveedorDTO::razonSocial).orElse(""),
                    costo, estimado));
        }

        Reporte reporte = new Reporte("Productos a reponer",
                detalles(sucursal, filtros(estado, busqueda)),
                List.of(Columna.texto("Código"), Columna.texto("Producto"), Columna.numero("Stock"),
                        Columna.numero("Mínimo"), Columna.numero("Pto. pedido"), Columna.numero("Faltante"),
                        Columna.texto("Estado"), Columna.texto("Proveedor preferido"),
                        Columna.numero("Costo unit. $"), Columna.numero("Costo estimado $")),
                filas,
                lista.size() + " productos · costo estimado de reposición: $ "
                        + costoTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
        return exportar(reporte, "a-reponer", sucursal, formato);
    }

    private ArchivoReporte exportar(Reporte reporte, String nombre, Sucursal sucursal, FormatoReporte formato) {
        ExportadorReporte exportador = exportadores.get(formato);
        String archivo = nombre + "-" + sucursal.getCodigo().toLowerCase() + "-" + LocalDate.now() + "." + formato.extension();
        return new ArchivoReporte(archivo, formato.contentType(), exportador.exportar(reporte));
    }

    private static List<String> detalles(Sucursal sucursal, List<String> filtros) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Ferretería Bulón · " + sucursal.getNombre() + " · generado el " + LocalDateTime.now().format(FECHA_HORA));
        lineas.addAll(filtros);
        return lineas;
    }

    private static List<String> filtros(EstadoStock estado, String busqueda) {
        List<String> filtros = new ArrayList<>();
        if (estado != null) {
            filtros.add("Estado: " + ESTADO.get(estado));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            filtros.add("Búsqueda: \"" + busqueda.trim() + "\"");
        }
        return filtros;
    }

    private static String detalle(MovimientoDetalleDTO m) {
        String texto = m.motivo() != null ? m.motivo() : m.observaciones();
        if (m.referencia() == null) {
            return texto != null ? texto : "";
        }
        return texto != null ? texto + " · " + m.referencia() : m.referencia();
    }

    private static BigDecimal faltante(StockSucursalDTO s) {
        return s.puntoPedido().subtract(s.cantidad()).max(BigDecimal.ZERO);
    }

    private static long contar(List<StockSucursalDTO> lista, EstadoStock estado) {
        return lista.stream().filter(s -> s.estado() == estado).count();
    }
}
