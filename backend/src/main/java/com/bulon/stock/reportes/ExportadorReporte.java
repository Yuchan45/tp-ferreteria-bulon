package com.bulon.stock.reportes;

/**
 * Patrón Adapter: la interfaz que espera el sistema (Target). Cada implementación adapta
 * una librería externa (Adaptee) con su propia API —OpenPDF, Apache POI— a esta firma única,
 * así {@link ReporteService} exporta sin saber qué librería hay detrás.
 */
public interface ExportadorReporte {

    FormatoReporte formato();

    byte[] exportar(Reporte reporte);
}
