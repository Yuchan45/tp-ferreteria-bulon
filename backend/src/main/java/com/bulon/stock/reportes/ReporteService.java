package com.bulon.stock.reportes;

import com.bulon.stock.alertas.AlertaService;
import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.FiltroMovimientos;
import com.bulon.stock.movimientos.MovimientoDTO;
import com.bulon.stock.movimientos.MovimientoStockRepository;
import com.bulon.stock.movimientos.MovimientoStockService;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.productos.ProductoDTO;
import com.bulon.stock.productos.ProductoService;
import com.bulon.stock.reportes.export.FormatoReporte;
import com.bulon.stock.reportes.export.Reporte;
import com.bulon.stock.reportes.export.ReporteExporter;
import com.bulon.stock.stock.ExistenciaDTO;
import com.bulon.stock.stock.StockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final int MAX_FILAS = 5_000;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductoService productoService;
    private final MovimientoStockService movimientoService;
    private final MovimientoStockRepository movimientoRepository;
    private final StockService stockService;
    private final AlertaService alertaService;
    private final Map<FormatoReporte, ReporteExporter> exporters;
    private final int diasEntrega;

    public ReporteService(ProductoService productoService,
                          MovimientoStockService movimientoService,
                          MovimientoStockRepository movimientoRepository,
                          StockService stockService,
                          AlertaService alertaService,
                          List<ReporteExporter> exporters,
                          @Value("${app.reposicion.dias-entrega:7}") int diasEntrega) {
        this.productoService = productoService;
        this.movimientoService = movimientoService;
        this.movimientoRepository = movimientoRepository;
        this.stockService = stockService;
        this.alertaService = alertaService;
        this.exporters = exporters.stream().collect(Collectors.toMap(ReporteExporter::formato, Function.identity()));
        this.diasEntrega = diasEntrega;
    }

    public DashboardDTO dashboard() {
        return new DashboardDTO(
                productoService.contarActivos(),
                productoService.contarPendientesReposicion(),
                productoService.contarBajoMinimo(),
                alertaService.contarAbiertas(),
                movimientoService.contarDesde(LocalDate.now()),
                stockService.unidadesPorUbicacion(),
                movimientoService.listar(sinFiltros(), PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "fecha")))
                        .getContent());
    }

    /** Productos más vendidos en [desde, hasta] (por defecto, los últimos 30 días). */
    public List<RotacionDTO> rotacion(LocalDate desde, LocalDate hasta, int limite) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        if (inicio.isAfter(fin)) {
            throw new BusinessException("La fecha desde no puede ser posterior a hasta");
        }
        long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;

        return movimientoRepository.unidadesVendidas(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(),
                        PageRequest.of(0, limite)).stream()
                .map(fila -> {
                    Producto p = productoService.buscarEntidad((Long) fila[0]);
                    long vendidas = ((Number) fila[1]).longValue();
                    int sugerido = (int) Math.ceil((double) vendidas / dias * diasEntrega) + p.getStockMinimo();
                    return new RotacionDTO(p.getId(), p.getCodigo(), p.getNombre(), vendidas,
                            stockService.total(p.getId()), p.getPuntoPedido(), sugerido);
                })
                .toList();
    }

    public Archivo exportarMovimientos(FiltroMovimientos filtro, FormatoReporte formato) {
        List<List<String>> filas = movimientoService
                .listar(filtro, PageRequest.of(0, MAX_FILAS, Sort.by(Sort.Direction.DESC, "fecha")))
                .map(m -> List.of(m.fecha().format(FECHA_HORA), m.tipo().name(), m.productoCodigo(),
                        m.productoNombre(), String.valueOf(m.cantidad()),
                        m.ubicacionDestino() != null ? m.ubicacion() + " → " + m.ubicacionDestino() : m.ubicacion(),
                        m.usuario(), Objects.toString(m.motivo(), ""), Objects.toString(m.observacion(), "")))
                .getContent();
        return exportar(new Reporte("Movimientos de stock · " + LocalDate.now(),
                List.of("Fecha", "Tipo", "Código", "Producto", "Cantidad", "Ubicación", "Usuario", "Motivo",
                        "Observación"), filas), formato, "movimientos");
    }

    public Archivo exportarInventario(FormatoReporte formato) {
        List<ProductoDTO> productos = productoService
                .listar(null, null, true, false, PageRequest.of(0, MAX_FILAS, Sort.by("codigo"))).getContent();
        List<String> ubicaciones = productos.isEmpty() ? List.of()
                : productos.getFirst().existencias().stream().map(ExistenciaDTO::ubicacion).toList();
        List<List<String>> filas = productos.stream()
                .map(p -> Stream.of(
                                Stream.of(p.codigo(), p.nombre(), p.categoria()),
                                p.existencias().stream().map(e -> String.valueOf(e.cantidad())),
                                Stream.of(String.valueOf(p.stockTotal()), String.valueOf(p.stockMinimo()),
                                        String.valueOf(p.puntoPedido()), p.estadoStock().name()))
                        .flatMap(Function.identity()).toList())
                .toList();
        List<String> columnas = new ArrayList<>(List.of("Código", "Producto", "Categoría"));
        columnas.addAll(ubicaciones);
        columnas.addAll(List.of("Total", "Mínimo", "Punto de pedido", "Estado"));
        return exportar(new Reporte("Inventario · " + LocalDate.now(), columnas, filas), formato, "inventario");
    }

    private Archivo exportar(Reporte reporte, FormatoReporte formato, String nombre) {
        ReporteExporter exporter = exporters.get(formato);
        return new Archivo(nombre + "-" + LocalDate.now() + "." + exporter.extension(), exporter.tipoContenido(),
                exporter.exportar(reporte));
    }

    private static FiltroMovimientos sinFiltros() {
        return new FiltroMovimientos(null, null, null, null, null, null);
    }

    public record Archivo(String nombre, String tipoContenido, byte[] contenido) {
    }
}
