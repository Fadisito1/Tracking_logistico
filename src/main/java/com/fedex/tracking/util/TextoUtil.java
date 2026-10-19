package com.fedex.tracking.util;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades de texto para aceptar etiquetas del front ("En tránsito") y códigos del back (EN_TRANSITO). */
public final class TextoUtil {

    private TextoUtil() {
    }

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
