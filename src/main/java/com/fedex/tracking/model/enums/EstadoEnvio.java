package com.fedex.tracking.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fedex.tracking.util.TextoUtil;

public enum EstadoEnvio {
    PENDIENTE_RECOGIDA("Pendiente de recogida"),
    RECOGIDO("Recogido"),
    EN_TRANSITO("En tránsito"),
    EN_CENTRO_DISTRIBUCION("En centro de distribución"),
    EN_RUTA_ENTREGA("En ruta de entrega"),
    ENTREGADO("Entregado"),
    INCIDENCIA("Incidencia"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoEnvio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Estados en los que el envío todavía puede editarse o eliminarse (regla del front y HU03). */
    public boolean permiteModificar() {
        return this == PENDIENTE_RECOGIDA || this == RECOGIDO;
    }

    /** Envío en curso: ni entregado ni cancelado. */
    public boolean esActivo() {
        return this != ENTREGADO && this != CANCELADO;
    }

    public boolean esFinal() {
        return this == ENTREGADO || this == CANCELADO;
    }

    /** Acepta el código (EN_TRANSITO) o la etiqueta del front ("En tránsito"), sin importar tildes ni mayúsculas. */
    @JsonCreator
    public static EstadoEnvio fromTexto(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String buscado = TextoUtil.normalizar(texto);
        for (EstadoEnvio e : values()) {
            if (TextoUtil.normalizar(e.name()).equals(buscado) || TextoUtil.normalizar(e.etiqueta).equals(buscado)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de envío no válido: " + texto);
    }
}
