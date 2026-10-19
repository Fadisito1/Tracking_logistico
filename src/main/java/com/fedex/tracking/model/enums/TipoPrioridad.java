package com.fedex.tracking.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fedex.tracking.util.TextoUtil;

public enum TipoPrioridad {
    ESTANDAR("Estándar"),
    EXPRES("Exprés"),
    PROGRAMADO("Programado");

    private final String etiqueta;

    TipoPrioridad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Acepta el código (EXPRES) o la etiqueta del front ("Exprés"). */
    @JsonCreator
    public static TipoPrioridad fromTexto(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String buscado = TextoUtil.normalizar(texto);
        for (TipoPrioridad p : values()) {
            if (TextoUtil.normalizar(p.name()).equals(buscado) || TextoUtil.normalizar(p.etiqueta).equals(buscado)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Tipo de envío no válido: " + texto);
    }
}
