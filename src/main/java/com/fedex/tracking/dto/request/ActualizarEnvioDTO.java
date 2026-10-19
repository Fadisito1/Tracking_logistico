package com.fedex.tracking.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fedex.tracking.model.enums.TipoPrioridad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Todos los campos son opcionales: solo se modifica lo que venga informado. */
@Data
public class ActualizarEnvioDTO {

    // Edición con datos (como lo hace el formulario del front)
    @Valid
    private ClienteActualizarDTO remitente;
    @Valid
    private ClienteActualizarDTO destinatario;
    @Valid
    private DireccionActualizarDTO direccionOrigen;
    @Valid
    private DireccionActualizarDTO direccionDestino;

    // Edición por ID (API anterior)
    private Long idRemitente;
    private Long idDestinatario;
    private Long idDireccionOrigen;
    private Long idDireccionDestino;

    private TipoPrioridad prioridad;

    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso;

    private String dimensiones;

    @Positive(message = "El largo debe ser mayor a 0")
    private Double largo;

    @Positive(message = "El ancho debe ser mayor a 0")
    private Double ancho;

    @Positive(message = "El alto debe ser mayor a 0")
    private Double alto;

    @JsonAlias({"fechaProgramada", "scheduledDate"})
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}(:\\d{2})?)?",
            message = "La fecha estimada debe usar formato ISO (yyyy-MM-dd o yyyy-MM-dd'T'HH:mm:ss)")
    private String fechaEstimadaEntrega;

    @Data
    public static class ClienteActualizarDTO {
        @Size(max = 150)
        private String nombre;
        @Size(max = 50)
        private String telefono;
        @Email(message = "El email del cliente no es válido")
        @Size(max = 150)
        private String email;
    }

    @Data
    public static class DireccionActualizarDTO {
        @Size(max = 200)
        private String calle;
        @Size(max = 30)
        private String numero;
        @Size(max = 100)
        private String ciudad;
        @Size(max = 100)
        private String departamentoProvincia;
        @Size(max = 20)
        private String codigoPostal;
        @Size(max = 100)
        private String pais;
    }
}
