package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.MovimientoEnvio;
import com.fedex.tracking.model.enums.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoEnvioRepository extends JpaRepository<MovimientoEnvio, Long> {
    List<MovimientoEnvio> findByEnvioIdOrderByFechaHoraAsc(Long envioId);

    long countByEstadoAndFechaHoraBetween(EstadoEnvio estado, LocalDateTime desde, LocalDateTime hasta);

    /** Pares {fecha de registro del envío, fecha del movimiento} para calcular el tiempo promedio de entrega. */
    @Query("select m.envio.fechaRegistro, m.fechaHora from MovimientoEnvio m where m.estado = :estado")
    List<Object[]> fechasRegistroYMovimiento(@Param("estado") EstadoEnvio estado);
}
