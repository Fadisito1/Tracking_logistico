package com.fedex.tracking.controller;

import com.fedex.tracking.dto.request.ActualizarEnvioDTO;
import com.fedex.tracking.dto.request.CrearEnvioDTO;
import com.fedex.tracking.dto.request.FiltroEnvioDTO;
import com.fedex.tracking.dto.request.RegistrarMovimientoDTO;
import com.fedex.tracking.dto.response.AuditoriaDTO;
import com.fedex.tracking.dto.response.EnvioDetalleDTO;
import com.fedex.tracking.dto.response.EstadisticasEnvioDTO;
import com.fedex.tracking.dto.response.MovimientoEnvioDTO;
import com.fedex.tracking.dto.response.RespuestaPaginadaDTO;
import com.fedex.tracking.service.IEnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/envios")
@RequiredArgsConstructor
@Tag(name = "Envíos", description = "API para la gestión de envíos (Tracking Logístico)")
public class EnvioController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final IEnvioService envioService;

    @PostMapping
    @Operation(summary = "Crear un nuevo envío", description = "Genera el envío y calcula su costo y fecha estimada. La dirección de origen y las dimensiones son opcionales.")
    public ResponseEntity<EnvioDetalleDTO> crearEnvio(@Valid @RequestBody CrearEnvioDTO dto, @RequestParam(defaultValue = "1") Long operadorId) {
        EnvioDetalleDTO creado = envioService.crearEnvio(dto, operadorId);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @GetMapping("/tracking/{codigo}")
    @Operation(summary = "Rastrear envío por Tracking Number", description = "Búsqueda pública por código LOG-XXXXXXXX-X (con o sin guiones)")
    public ResponseEntity<EnvioDetalleDTO> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(envioService.rastrearPorTrackingNumber(codigo));
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Resumen para el dashboard", description = "Envíos activos, entregados hoy, retenidos, tiempo promedio y conteo por estado")
    public ResponseEntity<EstadisticasEnvioDTO> estadisticas() {
        return ResponseEntity.ok(envioService.obtenerEstadisticas());
    }

    @GetMapping("/export")
    @Operation(summary = "Exportar listado", description = "Descarga el listado filtrado en Excel (formato=xlsx, por defecto) o CSV (formato=csv)")
    public ResponseEntity<byte[]> exportarReporte(@ModelAttribute FiltroEnvioDTO filtro,
                                                  @RequestParam(defaultValue = "xlsx") String formato) {
        boolean csv = "csv".equalsIgnoreCase(formato);
        byte[] contenido = csv ? envioService.exportarEnviosCsv(filtro) : envioService.exportarEnviosExcel(filtro);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(csv ? CSV : XLSX);
        headers.setContentDisposition(ContentDisposition.attachment().filename(csv ? "envios.csv" : "envios.xlsx").build());
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un envío (incluye historial y auditoría)")
    public ResponseEntity<EnvioDetalleDTO> obtenerEnvio(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.obtenerEnvio(id));
    }

    @GetMapping("/{id}/movimientos")
    public ResponseEntity<List<MovimientoEnvioDTO>> listarMovimientos(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.listarMovimientos(id));
    }

    @PostMapping("/{id}/movimientos")
    @Operation(summary = "Registrar un movimiento", description = "Cambia el estado operativo del envío y lo agrega al historial. No aplica a envíos entregados o cancelados.")
    public ResponseEntity<MovimientoEnvioDTO> registrarMovimiento(@PathVariable Long id, @Valid @RequestBody RegistrarMovimientoDTO dto) {
        return new ResponseEntity<>(envioService.registrarMovimiento(id, dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/auditoria")
    @Operation(summary = "Historial de ediciones del envío", description = "Más reciente primero")
    public ResponseEntity<List<AuditoriaDTO>> listarAuditoria(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.listarAuditoria(id));
    }

    @GetMapping
    @Operation(summary = "Listar envíos con filtros y paginación",
            description = "page es base 0. Filtros: textoBusqueda, estado, tipo (código o etiqueta), fechaInicio y fechaFin (yyyy-MM-dd). "
                    + "sort acepta trackingCode, senderName, recipientName, status, type, registeredAt (o los nombres de la entidad). "
                    + "Los envíos cancelados no se listan salvo que estado=CANCELADO.")
    public ResponseEntity<RespuestaPaginadaDTO<EnvioDetalleDTO>> listarEnvios(
            @ModelAttribute FiltroEnvioDTO filtro,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(envioService.listarEnvios(filtro, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos del envío (Auditoría)", description = "Solo en estado Pendiente de recogida o Recogido. Solo se modifica lo informado.")
    public ResponseEntity<EnvioDetalleDTO> actualizarEnvio(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEnvioDTO dto,
            @RequestParam(defaultValue = "Admin") String usuario) {
        return ResponseEntity.ok(envioService.actualizarEnvio(id, dto, usuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrado lógico (Cancelar envío)", description = "Solo permitido en estado Pendiente de recogida o Recogido")
    public ResponseEntity<Void> eliminarEnvio(@PathVariable Long id, @RequestParam(defaultValue = "Admin") String usuario) {
        envioService.eliminarEnvio(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
