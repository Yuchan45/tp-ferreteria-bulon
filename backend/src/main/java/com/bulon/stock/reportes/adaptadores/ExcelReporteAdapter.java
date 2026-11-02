package com.bulon.stock.reportes.adaptadores;

import com.bulon.stock.reportes.ExportadorReporte;
import com.bulon.stock.reportes.FormatoReporte;
import com.bulon.stock.reportes.Reporte;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Adapter de Excel: traduce un {@link Reporte} a la API de Apache POI (Workbook, Sheet,
 * Row, Cell...). Los números quedan como números (se pueden sumar o graficar), el
 * encabezado queda fijo al desplazarse y tiene autofiltro.
 */
@Component
public class ExcelReporteAdapter implements ExportadorReporte {

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.EXCEL;
    }

    @Override
    public byte[] exportar(Reporte reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName(reporte.titulo()));
            Estilos estilos = new Estilos(libro);
            List<Reporte.Columna> columnas = reporte.columnas();

            int fila = 0;
            celda(hoja.createRow(fila++), 0, reporte.titulo(), estilos.titulo);
            for (String linea : reporte.detalles()) {
                celda(hoja.createRow(fila++), 0, linea, estilos.detalle);
            }
            fila++;

            int filaEncabezado = fila;
            Row encabezado = hoja.createRow(fila++);
            for (int c = 0; c < columnas.size(); c++) {
                celda(encabezado, c, columnas.get(c).titulo(), estilos.encabezado);
            }

            for (List<Object> datos : reporte.filas()) {
                Row row = hoja.createRow(fila++);
                for (int c = 0; c < columnas.size(); c++) {
                    Object valor = datos.get(c);
                    if (valor instanceof Number numero) {
                        Cell cell = row.createCell(c);
                        cell.setCellValue(numero.doubleValue());
                        cell.setCellStyle(estilos.numero);
                    } else {
                        celda(row, c, valor == null ? "" : valor.toString(), null);
                    }
                }
            }

            if (reporte.resumen() != null) {
                celda(hoja.createRow(fila + 1), 0, reporte.resumen(), estilos.resumen);
            }

            hoja.createFreezePane(0, filaEncabezado + 1);
            if (!reporte.filas().isEmpty()) {
                hoja.setAutoFilter(new CellRangeAddress(filaEncabezado, fila - 1, 0, columnas.size() - 1));
            }
            for (int c = 0; c < columnas.size(); c++) {
                hoja.autoSizeColumn(c);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel", e);
        }
    }

    private static void celda(Row fila, int columna, String texto, CellStyle estilo) {
        Cell cell = fila.createCell(columna);
        cell.setCellValue(texto);
        if (estilo != null) {
            cell.setCellStyle(estilo);
        }
    }

    /** Estilos del libro (POI los crea por libro y conviene reutilizarlos). */
    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle detalle;
        final CellStyle encabezado;
        final CellStyle numero;
        final CellStyle resumen;

        Estilos(XSSFWorkbook libro) {
            titulo = libro.createCellStyle();
            titulo.setFont(fuente(libro, 14, true, IndexedColors.BLACK));

            detalle = libro.createCellStyle();
            detalle.setFont(fuente(libro, 10, false, IndexedColors.GREY_50_PERCENT));

            encabezado = libro.createCellStyle();
            encabezado.setFont(fuente(libro, 11, true, IndexedColors.WHITE));
            encabezado.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            numero = libro.createCellStyle();
            numero.setDataFormat(libro.createDataFormat().getFormat("#,##0.###"));
            numero.setAlignment(HorizontalAlignment.RIGHT);

            resumen = libro.createCellStyle();
            resumen.setFont(fuente(libro, 11, true, IndexedColors.BLACK));
        }

        private static XSSFFont fuente(XSSFWorkbook libro, int puntos, boolean negrita, IndexedColors color) {
            XSSFFont fuente = libro.createFont();
            fuente.setFontHeightInPoints((short) puntos);
            fuente.setBold(negrita);
            fuente.setColor(color.getIndex());
            return fuente;
        }
    }
}
