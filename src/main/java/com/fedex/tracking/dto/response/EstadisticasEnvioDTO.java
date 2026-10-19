package com.fedex.tracking.dto.response;

import com.fedex.tracking.model.enums.EstadoEnvio;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EstadisticasEnvioDTO {
    /** Envíos que no están entregados ni cancelados. */
    private long enviosActivos;
    private long entregadosHoy;
    /** Envíos en estado INCIDENCIA. */
    private long retenidos;
    /** Días entre registro y entrega (null si aún no hay entregas). */
    private Double tiempoPromedioEntregaDias;
    private Map<EstadoEnvio, Long> conteoPorEstado;
}
