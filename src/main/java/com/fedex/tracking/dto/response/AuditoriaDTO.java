package com.fedex.tracking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AuditoriaDTO {
    private Long id;
    private String usuario;
    private List<String> campos;
    private LocalDateTime fechaHora;
}
