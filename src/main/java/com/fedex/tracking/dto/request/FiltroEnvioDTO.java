package com.fedex.tracking.dto.request;

import lombok.Data;

/**
 * Filtros del listado. Se reciben como texto para aceptar lo que envía el front:
 * estado/tipo como código (EN_TRANSITO) o etiqueta ("En tránsito"), y fechas como
 * yyyy-MM-dd (día completo) o yyyy-MM-dd'T'HH:mm:ss.
 */
@Data
public class FiltroEnvioDTO {
    private String textoBusqueda; // código, remitente, destinatario
    private String estado;
    private String tipo;
    private String fechaInicio;
    private String fechaFin;
}
