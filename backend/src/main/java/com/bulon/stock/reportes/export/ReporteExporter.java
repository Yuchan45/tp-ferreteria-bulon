package com.bulon.stock.reportes.export;

/**
 * Adapter (Entrega 2, 3.6): interfaz común para exportar un {@link Reporte}. Cada implementación
 * traduce el modelo interno a la API de una librería externa distinta (OpenPDF, fastexcel).
 */
public interface ReporteExporter {

    FormatoReporte formato();

    String extension();

    String tipoContenido();

    byte[] exportar(Reporte reporte);
}
