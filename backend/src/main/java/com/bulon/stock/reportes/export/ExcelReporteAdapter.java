package com.bulon.stock.reportes.export;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class ExcelReporteAdapter implements ReporteExporter {

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.EXCEL;
    }

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public String tipoContenido() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public byte[] exportar(Reporte reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (Workbook libro = new Workbook(salida, "Bulon", "1.0")) {
            Worksheet hoja = libro.newWorksheet("Reporte");
            for (int c = 0; c < reporte.columnas().size(); c++) {
                hoja.value(0, c, reporte.columnas().get(c));
                hoja.style(0, c).bold().fillColor("1F2933").fontColor("FFFFFF").set();
            }
            for (int f = 0; f < reporte.filas().size(); f++) {
                List<String> fila = reporte.filas().get(f);
                for (int c = 0; c < fila.size(); c++) {
                    escribir(hoja, f + 1, c, fila.get(c));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    /** Las columnas numéricas se escriben como número para poder sumarlas en Excel. */
    private static void escribir(Worksheet hoja, int fila, int columna, String valor) {
        if (valor != null && valor.matches("-?\\d+")) {
            hoja.value(fila, columna, Long.parseLong(valor));
        } else {
            hoja.value(fila, columna, valor);
        }
    }
}
