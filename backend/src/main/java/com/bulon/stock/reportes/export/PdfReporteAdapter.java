package com.bulon.stock.reportes.export;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Component
public class PdfReporteAdapter implements ReporteExporter {

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.PDF;
    }

    @Override
    public String extension() {
        return "pdf";
    }

    @Override
    public String tipoContenido() {
        return "application/pdf";
    }

    @Override
    public byte[] exportar(Reporte reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (Document documento = new Document(PageSize.A4.rotate(), 24, 24, 24, 24)) {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph(reporte.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(reporte.columnas().size());
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            Font encabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font celda = FontFactory.getFont(FontFactory.HELVETICA, 8);
            for (String columna : reporte.columnas()) {
                PdfPCell c = new PdfPCell(new Phrase(columna, encabezado));
                c.setBackgroundColor(new Color(31, 41, 51));
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(c);
            }
            for (var fila : reporte.filas()) {
                for (String valor : fila) {
                    tabla.addCell(new Phrase(valor, celda));
                }
            }
            documento.add(tabla);
        }
        return salida.toByteArray();
    }
}
