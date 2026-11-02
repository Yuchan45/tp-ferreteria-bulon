package com.bulon.stock.common;

import java.text.Normalizer;

/** Búsqueda de texto como la de la app: sin mayúsculas ni acentos ("lampara" encuentra "Lámpara"). */
public final class Busqueda {

    private Busqueda() {
    }

    /** true si la búsqueda está vacía o aparece en alguno de los textos. */
    public static boolean coincide(String busqueda, String... textos) {
        if (busqueda == null || busqueda.isBlank()) {
            return true;
        }
        String buscado = normalizar(busqueda.trim());
        for (String texto : textos) {
            if (texto != null && normalizar(texto).contains(buscado)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
