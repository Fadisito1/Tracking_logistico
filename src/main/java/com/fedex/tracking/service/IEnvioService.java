package com.fedex.tracking.service;

import com.fedex.tracking.dto.request.ActualizarEnvioDTO;
import com.fedex.tracking.dto.request.CrearEnvioDTO;
import com.fedex.tracking.dto.request.FiltroEnvioDTO;
import com.fedex.tracking.dto.request.RegistrarMovimientoDTO;
import com.fedex.tracking.dto.response.AuditoriaDTO;
import com.fedex.tracking.dto.response.EnvioDetalleDTO;
import com.fedex.tracking.dto.response.EstadisticasEnvioDTO;
import com.fedex.tracking.dto.response.MovimientoEnvioDTO;
import com.fedex.tracking.dto.response.RespuestaPaginadaDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IEnvioService {
    EnvioDetalleDTO crearEnvio(CrearEnvioDTO dto, Long operadorId);
    EnvioDetalleDTO rastrearPorTrackingNumber(String codigo);
    EnvioDetalleDTO obtenerEnvio(Long id);
    List<MovimientoEnvioDTO> listarMovimientos(Long id);
    MovimientoEnvioDTO registrarMovimiento(Long id, RegistrarMovimientoDTO dto);
    List<AuditoriaDTO> listarAuditoria(Long id);
    RespuestaPaginadaDTO<EnvioDetalleDTO> listarEnvios(FiltroEnvioDTO filtro, Pageable pageable);
    EnvioDetalleDTO actualizarEnvio(Long id, ActualizarEnvioDTO dto, String usuario);
    void eliminarEnvio(Long id, String usuario);
    byte[] exportarEnviosExcel(FiltroEnvioDTO filtro);
    byte[] exportarEnviosCsv(FiltroEnvioDTO filtro);
    EstadisticasEnvioDTO obtenerEstadisticas();
}
