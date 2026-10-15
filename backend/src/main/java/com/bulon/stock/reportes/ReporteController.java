package com.bulon.stock.reportes;

import com.bulon.stock.movimientos.FiltroMovimientos;
import com.bulon.stock.reportes.export.FormatoReporte;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/dashboard")
    public DashboardDTO dashboard() {
        return reporteService.dashboard();
    }

    @GetMapping("/reportes/rotacion")
    public List<RotacionDTO> rotacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return reporteService.rotacion(desde, hasta, Math.clamp(limite, 1, 100));
    }

    @GetMapping("/reportes/movimientos")
    public ResponseEntity<byte[]> exportarMovimientos(@ParameterObject FiltroMovimientos filtro,
                                                      @RequestParam FormatoReporte formato) {
        return descarga(reporteService.exportarMovimientos(filtro, formato));
    }

    @GetMapping("/reportes/inventario")
    public ResponseEntity<byte[]> exportarInventario(@RequestParam FormatoReporte formato) {
        return descarga(reporteService.exportarInventario(formato));
    }

    private static ResponseEntity<byte[]> descarga(ReporteService.Archivo archivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .body(archivo.contenido());
    }
}
