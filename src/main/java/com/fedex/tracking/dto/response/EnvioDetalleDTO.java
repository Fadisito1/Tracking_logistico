package com.fedex.tracking.dto.response;

import com.fedex.tracking.model.enums.EstadoEnvio;
import com.fedex.tracking.model.enums.TipoPrioridad;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EnvioDetalleDTO {
    private Long id;
    private String trackingNumber;
    private TipoPrioridad prioridad;
    /** Etiqueta para mostrar en pantalla: "Estándar", "Exprés", "Programado". */
    private String prioridadEtiqueta;
    private Double peso;
    /** Texto "LxAxH". */
    private String dimensiones;
    private Double largo;
    private Double ancho;
    private Double alto;
    private BigDecimal costo;
    private LocalDateTime fechaRegistro;
    /** Para envíos PROGRAMADO es la fecha elegida por el operador. */
    private LocalDateTime fechaEstimadaEntrega;
    private EstadoEnvio estadoOperativo;
    /** Etiqueta para mostrar en pantalla: "En tránsito", "Pendiente de recogida", etc. */
    private String estadoEtiqueta;

    private Long idRemitente;
    private String nombreRemitente;
    private String telefonoRemitente;
    private String emailRemitente;
    private DireccionDTO direccionOrigen;

    private Long idDestinatario;
    private String nombreDestinatario;
    private String telefonoDestinatario;
    private String emailDestinatario;
    private DireccionDTO direccionDestino;

    private List<MovimientoEnvioDTO> historialMovimientos;
    private String ubicacionActual;

    /** Solo se llena en el detalle por ID y tras editar. */
    private List<AuditoriaDTO> auditoria;
}
