package com.bulon.stock.reportes.export;

import java.util.List;

/** Modelo interno de un reporte tabular, independiente del formato de salida. */
public record Reporte(String titulo, List<String> columnas, List<List<String>> filas) {
}
