package com.fedex.tracking.dto.request;

import com.fedex.tracking.model.enums.EstadoEnvio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrarMovimientoDTO {

    @NotNull(message = "El estado es obligatorio")
    private EstadoEnvio estado;

    @Size(max = 250)
    private String ubicacion;

    @Size(max = 500)
    private String descripcion;
}
