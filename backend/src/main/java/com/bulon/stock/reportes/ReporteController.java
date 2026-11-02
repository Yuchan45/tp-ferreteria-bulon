package com.bulon.stock.reportes;

import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.reportes.ReporteService.ArchivoReporte;
import com.bulon.stock.stock.EstadoStock;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Descarga de reportes: {@code formato=pdf} o {@code formato=excel}; los filtros son opcionales. */
@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/stock")
    public ResponseEntity<byte[]> stock(@RequestParam Long sucursalId,
                                        @RequestParam(defaultValue = "pdf") String formato,
                                        @RequestParam(required = false) Long categoriaId,
                                        @RequestParam(required = false) EstadoStock estado,
                                        @RequestParam(required = false) String q) {
        return descarga(reporteService.stock(sucursalId, categoriaId, estado, q, FormatoReporte.desde(formato)));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<byte[]> movimientos(@RequestParam Long sucursalId,
                                              @RequestParam(defaultValue = "pdf") String formato,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @RequestParam(required = false) TipoMovimiento tipo,
                                              @RequestParam(required = false) String q) {
        return descarga(reporteService.movimientos(sucursalId, desde, hasta, tipo, q, FormatoReporte.desde(formato)));
    }

    @GetMapping("/a-reponer")
    public ResponseEntity<byte[]> aReponer(@RequestParam Long sucursalId,
                                           @RequestParam(defaultValue = "pdf") String formato,
                                           @RequestParam(required = false) EstadoStock estado,
                                           @RequestParam(required = false) String q) {
        return descarga(reporteService.aReponer(sucursalId, estado, q, FormatoReporte.desde(formato)));
    }

    private static ResponseEntity<byte[]> descarga(ArchivoReporte archivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                // El nombre es ASCII (tipo-sucursal-fecha), así que va sin codificar
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .body(archivo.contenido());
    }
}
