package com.bulon.stock.reportes.adaptadores;

import com.bulon.stock.reportes.ExportadorReporte;
import com.bulon.stock.reportes.FormatoReporte;
import com.bulon.stock.reportes.Reporte;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Adapter de PDF: traduce un {@link Reporte} a la API de OpenPDF (Document, PdfPTable,
 * PdfPCell...). Hoja horizontal, encabezado oscuro como el sidebar de la app y filas alternadas.
 */
@Component
public class PdfReporteAdapter implements ExportadorReporte {

    private static final Color OSCURO = new Color(0x1E, 0x29, 0x3B);
    private static final Color GRIS = new Color(0x64, 0x74, 0x8B);
    private static final Color FONDO_ALTERNO = new Color(0xF8, 0xFA, 0xFC);
    private static final Color BORDE = new Color(0xE2, 0xE8, 0xF0);

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.PDF;
    }

    @Override
    public byte[] exportar(Reporte reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        documento.add(new Paragraph(reporte.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, OSCURO)));
        Font detalle = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);
        for (String linea : reporte.detalles()) {
            documento.add(new Paragraph(linea, detalle));
        }

        documento.add(tabla(reporte));

        if (reporte.resumen() != null) {
            Paragraph resumen = new Paragraph(reporte.resumen(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, OSCURO));
            resumen.setSpacingBefore(8);
            documento.add(resumen);
        }
        documento.close();
        return salida.toByteArray();
    }

    private PdfPTable tabla(Reporte reporte) {
        List<Reporte.Columna> columnas = reporte.columnas();
        PdfPTable tabla = new PdfPTable(columnas.size());
        tabla.setWidthPercentage(100);
        tabla.setWidths(anchos(reporte));
        tabla.setSpacingBefore(12);
        tabla.setHeaderRows(1);   // el encabezado se repite en cada página

        Font encabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (Reporte.Columna columna : columnas) {
            PdfPCell celda = celda(new Phrase(columna.titulo(), encabezado), columna.numerica());
            celda.setBackgroundColor(OSCURO);
            tabla.addCell(celda);
        }

        Font texto = FontFactory.getFont(FontFactory.HELVETICA, 9, OSCURO);
        if (reporte.filas().isEmpty()) {
            PdfPCell vacia = celda(new Phrase("Sin datos para los filtros elegidos.", texto), false);
            vacia.setColspan(columnas.size());
            tabla.addCell(vacia);
        }
        for (int i = 0; i < reporte.filas().size(); i++) {
            List<Object> fila = reporte.filas().get(i);
            for (int c = 0; c < columnas.size(); c++) {
                PdfPCell celda = celda(new Phrase(formatear(fila.get(c)), texto), columnas.get(c).numerica());
                if (i % 2 == 1) {
                    celda.setBackgroundColor(FONDO_ALTERNO);
                }
                tabla.addCell(celda);
            }
        }
        return tabla;
    }

    /** Ancho relativo de cada columna según su encabezado y su texto más largo. */
    private static float[] anchos(Reporte reporte) {
        float[] anchos = new float[reporte.columnas().size()];
        for (int c = 0; c < anchos.length; c++) {
            // El encabezado va en negrita y no se corta: es el mínimo de la columna.
            // El contenido largo (nombres, detalles) se limita y pasa a otra línea.
            int encabezado = reporte.columnas().get(c).titulo().length() + 3;
            int largo = 0;
            for (List<Object> fila : reporte.filas()) {
                largo = Math.max(largo, formatear(fila.get(c)).length());
            }
            anchos[c] = Math.max(encabezado, Math.min(largo, 22));
        }
        return anchos;
    }

    private static PdfPCell celda(Phrase contenido, boolean numerica) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setPadding(5);
        celda.setBorderColor(BORDE);
        celda.setHorizontalAlignment(numerica ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return celda;
    }

    /** Números con formato argentino (1.234,5); el resto, como texto. */
    private static String formatear(Object valor) {
        if (valor == null) {
            return "—";
        }
        if (valor instanceof Number numero) {
            DecimalFormat formato = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.of("es", "AR")));
            return formato.format(numero instanceof BigDecimal bd ? bd : new BigDecimal(numero.toString()));
        }
        return valor.toString();
    }
}
