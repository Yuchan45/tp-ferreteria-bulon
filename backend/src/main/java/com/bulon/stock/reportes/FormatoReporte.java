package com.bulon.stock.reportes;

import com.bulon.stock.common.exception.BusinessException;

public enum FormatoReporte {
    PDF("application/pdf", "pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String contentType;
    private final String extension;

    FormatoReporte(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    /** Desde el parámetro de la URL: {@code pdf} o {@code excel}, sin importar mayúsculas. */
    public static FormatoReporte desde(String valor) {
        for (FormatoReporte formato : values()) {
            if (formato.name().equalsIgnoreCase(valor)) {
                return formato;
            }
        }
        throw new BusinessException("Formato de reporte desconocido: " + valor + " (usar pdf o excel)");
    }
}
