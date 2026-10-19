package com.fedex.tracking.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fedex.tracking.model.enums.TipoPrioridad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearEnvioDTO {

    @Valid
    private ClienteEnvioDTO remitente;

    @Valid
    private ClienteEnvioDTO destinatario;

    /** Opcional: el formulario del front solo captura la dirección de destino. */
    @Valid
    private DireccionEnvioDTO direccionOrigen;

    @Valid
    private DireccionEnvioDTO direccionDestino;

    // IDs retenidos para clientes de la API anterior.
    private Long idRemitente;
    private Long idDestinatario;
    private Long idDireccionOrigen;
    private Long idDireccionDestino;

    /** Acepta ESTANDAR / EXPRES / PROGRAMADO o las etiquetas del front (Estándar / Exprés / Programado). */
    @NotNull(message = "La prioridad es obligatoria")
    private TipoPrioridad prioridad;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso;

    /** Opcional. Alternativa a largo/ancho/alto, con formato "LxAxH" (ej. 40x30x20). */
    private String dimensiones;

    @Positive(message = "El largo debe ser mayor a 0")
    private Double largo;

    @Positive(message = "El ancho debe ser mayor a 0")
    private Double ancho;

    @Positive(message = "El alto debe ser mayor a 0")
    private Double alto;

    // Obligatoria solo si es PROGRAMADO. El front la llama scheduledDate.
    @JsonAlias({"fechaProgramada", "scheduledDate"})
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}(:\\d{2})?)?",
            message = "La fecha estimada debe usar formato ISO (yyyy-MM-dd o yyyy-MM-dd'T'HH:mm:ss)")
    private String fechaEstimadaEntrega;

    @Data
    public static class ClienteEnvioDTO {
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 150)
        private String nombre;
        @NotBlank(message = "El teléfono del cliente es obligatorio")
        @Size(max = 50)
        private String telefono;
        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "El email del cliente no es válido")
        @Size(max = 150)
        private String email;
    }

    @Data
    public static class DireccionEnvioDTO {
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 200)
        private String calle;
        @NotBlank(message = "El número es obligatorio")
        @Size(max = 30)
        private String numero;
        @NotBlank(message = "La ciudad es obligatoria")
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
