package com.bulon.stock.reportes;

import java.util.List;

/**
 * Un reporte armado, independiente del formato: título, líneas de contexto (sucursal,
 * período, filtros), columnas, filas y un resumen al pie. Los exportadores lo traducen a
 * PDF o Excel; el negocio nunca toca las librerías.
 *
 * @param filas cada celda es un {@code String} o un {@code Number} (los números se alinean
 *              a la derecha y en Excel quedan como números, para poder sumarlos)
 */
public record Reporte(
        String titulo,
        List<String> detalles,
        List<Columna> columnas,
        List<List<Object>> filas,
        String resumen
) {

    public record Columna(String titulo, boolean numerica) {

        public static Columna texto(String titulo) {
            return new Columna(titulo, false);
        }

        public static Columna numero(String titulo) {
            return new Columna(titulo, true);
        }
    }
}
