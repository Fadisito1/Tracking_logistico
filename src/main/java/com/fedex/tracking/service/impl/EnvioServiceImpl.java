package com.fedex.tracking.service.impl;

import com.fedex.tracking.dto.request.ActualizarEnvioDTO;
import com.fedex.tracking.dto.request.CrearEnvioDTO;
import com.fedex.tracking.dto.request.FiltroEnvioDTO;
import com.fedex.tracking.dto.request.RegistrarMovimientoDTO;
import com.fedex.tracking.dto.response.AuditoriaDTO;
import com.fedex.tracking.dto.response.DireccionDTO;
import com.fedex.tracking.dto.response.EnvioDetalleDTO;
import com.fedex.tracking.dto.response.EstadisticasEnvioDTO;
import com.fedex.tracking.dto.response.MovimientoEnvioDTO;
import com.fedex.tracking.dto.response.RespuestaPaginadaDTO;
import com.fedex.tracking.exception.BusinessRuleException;
import com.fedex.tracking.exception.ResourceNotFoundException;
import com.fedex.tracking.model.entity.*;
import com.fedex.tracking.model.enums.EstadoEnvio;
import com.fedex.tracking.model.enums.TipoPrioridad;
import com.fedex.tracking.repository.*;
import com.fedex.tracking.service.IEnvioService;
import com.fedex.tracking.util.DimensionesUtil;
import com.fedex.tracking.util.TrackingNumberGenerator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnvioServiceImpl implements IEnvioService {

    private static final String ORIGEN_NO_ESPECIFICADO = "Origen no especificado";

    /** Nombres de columna que usa el front (y los del DTO) -> propiedad real de la entidad. */
    private static final Map<String, String> ALIAS_ORDEN = Map.ofEntries(
            Map.entry("trackingCode", "trackingNumber"),
            Map.entry("codigo", "trackingNumber"),
            Map.entry("senderName", "remitente.nombre"),
            Map.entry("nombreRemitente", "remitente.nombre"),
            Map.entry("recipientName", "destinatario.nombre"),
            Map.entry("nombreDestinatario", "destinatario.nombre"),
            Map.entry("status", "estadoOperativo"),
            Map.entry("estado", "estadoOperativo"),
            Map.entry("type", "prioridad"),
            Map.entry("tipo", "prioridad"),
            Map.entry("registeredAt", "fechaRegistro"),
            Map.entry("weight", "peso"));

    private static final Set<String> CAMPOS_ORDEN = Set.of(
            "id", "trackingNumber", "prioridad", "peso", "costo", "fechaRegistro",
            "fechaEstimadaEntrega", "estadoOperativo", "remitente.nombre", "destinatario.nombre");

    private final EnvioRepository envioRepository;
    private final ClienteRepository clienteRepository;
    private final DireccionRepository direccionRepository;
    private final PersonalLogisticoRepository personalLogisticoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final TrackingNumberGenerator trackingGenerator;
    private final MovimientoEnvioRepository movimientoRepository;
    private final TrackingNumberSequenceRepository trackingSequenceRepository;

    // ------------------------------------------------------------------ crear

    @Override
    @Transactional
    public EnvioDetalleDTO crearEnvio(CrearEnvioDTO dto, Long operadorId) {
        LocalDateTime fechaProgramada = null;
        if (dto.getPrioridad() == TipoPrioridad.PROGRAMADO) {
            if (dto.getFechaEstimadaEntrega() == null || dto.getFechaEstimadaEntrega().isBlank()) {
                throw new BusinessRuleException("La fecha estimada es obligatoria para envíos programados");
            }
            fechaProgramada = parseFechaEstimada(dto.getFechaEstimadaEntrega());
            validarFechaProgramada(fechaProgramada);
        }

        Cliente remitente = resolveCliente(dto.getRemitente(), dto.getIdRemitente(), "remitente");
        Cliente destinatario = resolveCliente(dto.getDestinatario(), dto.getIdDestinatario(), "destinatario");
        Direccion dirOrigen = resolveDireccionOpcional(dto.getDireccionOrigen(), dto.getIdDireccionOrigen());
        Direccion dirDestino = resolveDireccion(dto.getDireccionDestino(), dto.getIdDireccionDestino(), "destino");
        PersonalLogistico operador = getOperador(operadorId);
        String dimensiones = dimensionesParaCrear(dto);

        Envio envio = Envio.builder()
                .trackingNumber(nextTrackingNumber())
                .prioridad(dto.getPrioridad())
                .peso(dto.getPeso())
                .dimensiones(dimensiones)
                .remitente(remitente)
                .destinatario(destinatario)
                .direccionOrigen(dirOrigen)
                .direccionDestino(dirDestino)
                .operadorCreador(operador)
                .build();
        if (fechaProgramada != null) {
            envio.setFechaEstimadaEntrega(fechaProgramada);
        }

        envio = envioRepository.save(envio);
        String ubicacionInicial = dirOrigen != null ? dirOrigen.getCiudad() : ORIGEN_NO_ESPECIFICADO;
        guardarMovimiento(envio, envio.getEstadoOperativo(), "Envío registrado en sistema", ubicacionInicial);
        return mapToDTO(envio, false);
    }

    private String nextTrackingNumber() {
        TrackingNumberSequence sequence = trackingSequenceRepository.save(new TrackingNumberSequence());
        String tracking = TrackingNumberGenerator.generarCodigo(sequence.getId());
        while (envioRepository.existsByTrackingNumberIgnoreCase(tracking)) {
            sequence = trackingSequenceRepository.save(new TrackingNumberSequence());
            tracking = TrackingNumberGenerator.generarCodigo(sequence.getId());
        }
        return tracking;
    }

    // ------------------------------------------------------------------ consultas

    @Override
    @Transactional(readOnly = true)
    public EnvioDetalleDTO obtenerEnvio(Long id) {
        return mapToDTO(buscar(id), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoEnvioDTO> listarMovimientos(Long id) {
        if (!envioRepository.existsById(id)) throw new ResourceNotFoundException("Envío no encontrado");
        return movimientoRepository.findByEnvioIdOrderByFechaHoraAsc(id).stream().map(this::mapMovimiento).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listarAuditoria(Long id) {
        if (!envioRepository.existsById(id)) throw new ResourceNotFoundException("Envío no encontrado");
        return auditoriaRepository.findByEnvioIdOrderByFechaHoraDesc(id).stream().map(this::mapAuditoria).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioDetalleDTO rastrearPorTrackingNumber(String codigo) {
        if (!trackingGenerator.validarFormato(codigo)) {
            throw new BusinessRuleException("El código ingresado no es válido. Verifica el formato.");
        }
        String codigoNormalizado = trackingGenerator.normalizarCodigo(codigo);

        Envio envio = envioRepository.findByTrackingNumberIgnoreCase(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ningún envío con el código ingresado. Verifica que sea correcto."));

        return mapToDTO(envio, false);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPaginadaDTO<EnvioDetalleDTO> listarEnvios(FiltroEnvioDTO filtro, Pageable pageable) {
        Page<Envio> page = envioRepository.findAll(construirSpecification(filtro), normalizarPaginacion(pageable));
        List<EnvioDetalleDTO> dtos = page.getContent().stream().map(e -> mapToDTO(e, false)).toList();
        return new RespuestaPaginadaDTO<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public EstadisticasEnvioDTO obtenerEstadisticas() {
        Map<EstadoEnvio, Long> conteo = new EnumMap<>(EstadoEnvio.class);
        for (EstadoEnvio e : EstadoEnvio.values()) conteo.put(e, 0L);
        for (Object[] fila : envioRepository.contarPorEstado()) {
            conteo.put((EstadoEnvio) fila[0], ((Number) fila[1]).longValue());
        }
        long activos = conteo.entrySet().stream()
                .filter(en -> en.getKey().esActivo()).mapToLong(Map.Entry::getValue).sum();

        LocalDate hoy = LocalDate.now();
        long entregadosHoy = movimientoRepository.countByEstadoAndFechaHoraBetween(
                EstadoEnvio.ENTREGADO, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());

        double totalMinutos = 0;
        int entregas = 0;
        for (Object[] fila : movimientoRepository.fechasRegistroYMovimiento(EstadoEnvio.ENTREGADO)) {
            totalMinutos += Duration.between((LocalDateTime) fila[0], (LocalDateTime) fila[1]).toMinutes();
            entregas++;
        }
        Double promedioDias = entregas == 0 ? null : Math.round(totalMinutos / entregas / 1440.0 * 10) / 10.0;

        return EstadisticasEnvioDTO.builder()
                .enviosActivos(activos)
                .entregadosHoy(entregadosHoy)
                .retenidos(conteo.get(EstadoEnvio.INCIDENCIA))
                .tiempoPromedioEntregaDias(promedioDias)
                .conteoPorEstado(conteo)
                .build();
    }

    // ------------------------------------------------------------------ movimientos

    @Override
    @Transactional
    public MovimientoEnvioDTO registrarMovimiento(Long id, RegistrarMovimientoDTO dto) {
        Envio envio = buscar(id);
        if (envio.getEstadoOperativo().esFinal()) {
            throw new BusinessRuleException("El envío ya está en un estado final (" +
                    envio.getEstadoOperativo().getEtiqueta() + ") y no admite más movimientos");
        }
        if (dto.getEstado() == EstadoEnvio.CANCELADO) {
            throw new BusinessRuleException("Para cancelar un envío use la opción de eliminar");
        }
        envio.setEstadoOperativo(dto.getEstado());
        envioRepository.save(envio);
        MovimientoEnvio mov = guardarMovimiento(envio, dto.getEstado(), dto.getDescripcion(), dto.getUbicacion());
        return mapMovimiento(mov);
    }

    // ------------------------------------------------------------------ actualizar

    @Override
    @Transactional
    public EnvioDetalleDTO actualizarEnvio(Long id, ActualizarEnvioDTO dto, String usuario) {
        Envio envio = buscar(id);
        if (!envio.esEditable()) {
            throw new BusinessRuleException("Solo se pueden editar envíos en estado Pendiente de recogida o Recogido");
        }

        List<String> cambios = new ArrayList<>();
        boolean recalcularCosto = false;

        // --- tipo de envío y fecha programada
        TipoPrioridad nueva = dto.getPrioridad() != null ? dto.getPrioridad() : envio.getPrioridad();
        LocalDateTime fechaDto = (dto.getFechaEstimadaEntrega() == null || dto.getFechaEstimadaEntrega().isBlank())
                ? null : parseFechaEstimada(dto.getFechaEstimadaEntrega());
        boolean cambioPrioridad = nueva != envio.getPrioridad();
        if (nueva == TipoPrioridad.PROGRAMADO && cambioPrioridad && fechaDto == null) {
            throw new BusinessRuleException("La fecha estimada es obligatoria para envíos programados");
        }
        boolean cambioFecha = nueva == TipoPrioridad.PROGRAMADO && fechaDto != null
                && (cambioPrioridad || !fechaDto.equals(envio.getFechaEstimadaEntrega()));
        if (cambioFecha) validarFechaProgramada(fechaDto);

        if (cambioPrioridad) {
            cambios.add("Tipo de envío modificado: " + envio.getPrioridad().getEtiqueta() + " -> " + nueva.getEtiqueta());
            envio.setPrioridad(nueva);
            envio.reiniciarFechaEstimada();
            recalcularCosto = true;
        }
        if (cambioFecha) {
            if (!cambioPrioridad) cambios.add("Fecha programada modificada");
            envio.setFechaEstimadaEntrega(fechaDto);
        }

        // --- peso y dimensiones
        if (dto.getPeso() != null && Double.compare(envio.getPeso(), dto.getPeso()) != 0) {
            cambios.add("Peso modificado: " + envio.getPeso() + " -> " + dto.getPeso());
            envio.setPeso(dto.getPeso());
            recalcularCosto = true;
        }
        String nuevasDimensiones = dimensionesParaActualizar(dto, envio.getDimensiones());
        if (nuevasDimensiones != null && !nuevasDimensiones.equals(envio.getDimensiones())) {
            cambios.add("Dimensiones modificadas: " + valorOVacio(envio.getDimensiones()) + " -> " + nuevasDimensiones);
            envio.setDimensiones(nuevasDimensiones);
        }
        if (recalcularCosto) envio.calcularCosto();

        // --- remitente / destinatario
        if (dto.getRemitente() != null) {
            envio.setRemitente(actualizarCliente(envio.getRemitente(), dto.getRemitente(), "remitente", cambios));
        } else if (dto.getIdRemitente() != null && !envio.getRemitente().getId().equals(dto.getIdRemitente())) {
            envio.setRemitente(getCliente(dto.getIdRemitente()));
            cambios.add("Remitente modificado");
        }
        if (dto.getDestinatario() != null) {
            envio.setDestinatario(actualizarCliente(envio.getDestinatario(), dto.getDestinatario(), "destinatario", cambios));
        } else if (dto.getIdDestinatario() != null && !envio.getDestinatario().getId().equals(dto.getIdDestinatario())) {
            envio.setDestinatario(getCliente(dto.getIdDestinatario()));
            cambios.add("Destinatario modificado");
        }

        // --- direcciones
        if (dto.getDireccionOrigen() != null) {
            envio.setDireccionOrigen(actualizarDireccion(envio.getDireccionOrigen(), dto.getDireccionOrigen(), "origen", cambios));
        } else if (dto.getIdDireccionOrigen() != null
                && (envio.getDireccionOrigen() == null || !envio.getDireccionOrigen().getId().equals(dto.getIdDireccionOrigen()))) {
            envio.setDireccionOrigen(getDireccion(dto.getIdDireccionOrigen()));
            cambios.add("Dirección de origen modificada");
        }
        if (dto.getDireccionDestino() != null) {
            envio.setDireccionDestino(actualizarDireccion(envio.getDireccionDestino(), dto.getDireccionDestino(), "destino", cambios));
        } else if (dto.getIdDireccionDestino() != null && !envio.getDireccionDestino().getId().equals(dto.getIdDireccionDestino())) {
            envio.setDireccionDestino(getDireccion(dto.getIdDireccionDestino()));
            cambios.add("Dirección de destino modificada");
        }

        if (!cambios.isEmpty()) {
            auditoriaRepository.save(RegistroAuditoria.builder()
                    .envioId(envio.getId())
                    .usuarioModificador(usuario)
                    .camposModificados(String.join("; ", cambios))
                    .fechaHora(LocalDateTime.now())
                    .build());
        }

        envio = envioRepository.save(envio);
        return mapToDTO(envio, true);
    }

    // ------------------------------------------------------------------ eliminar (borrado lógico)

    @Override
    @Transactional
    public void eliminarEnvio(Long id, String usuario) {
        Envio envio = buscar(id);

        if (!envio.esEliminable()) {
            throw new BusinessRuleException("Solo se pueden eliminar envíos en estado Pendiente de recogida o Recogido");
        }

        envio.setEstadoOperativo(EstadoEnvio.CANCELADO);
        envioRepository.save(envio);
        guardarMovimiento(envio, EstadoEnvio.CANCELADO, "Envío cancelado", ubicacionActual(envio.getId()));

        auditoriaRepository.save(RegistroAuditoria.builder()
                .envioId(envio.getId())
                .usuarioModificador(usuario)
                .camposModificados("Envío cancelado lógicamente")
                .fechaHora(LocalDateTime.now())
                .build());
    }

    // ------------------------------------------------------------------ exportación

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarEnviosExcel(FiltroEnvioDTO filtro) {
        List<Envio> envios = listarEnviosParaExportacion(filtro);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Envíos");

            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Tracking Number", "Tipo", "Estado", "Remitente", "Destinatario", "Fecha Registro", "Costo"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (Envio envio : envios) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(envio.getId());
                row.createCell(1).setCellValue(envio.getTrackingNumber());
                row.createCell(2).setCellValue(envio.getPrioridad().getEtiqueta());
                row.createCell(3).setCellValue(envio.getEstadoOperativo().getEtiqueta());
                row.createCell(4).setCellValue(envio.getRemitente().getNombre());
                row.createCell(5).setCellValue(envio.getDestinatario().getNombre());
                row.createCell(6).setCellValue(envio.getFechaRegistro().toString());
                row.createCell(7).setCellValue(envio.getCosto().doubleValue());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar Excel: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarEnviosCsv(FiltroEnvioDTO filtro) {
        StringBuilder csv = new StringBuilder("\uFEFF"); // BOM para que Excel respete las tildes
        csv.append("Código,Remitente,Destinatario,Estado,Tipo,Registrado\n");
        for (Envio e : listarEnviosParaExportacion(filtro)) {
            csv.append(String.join(",",
                            celdaCsv(e.getTrackingNumber()),
                            celdaCsv(e.getRemitente().getNombre()),
                            celdaCsv(e.getDestinatario().getNombre()),
                            celdaCsv(e.getEstadoOperativo().getEtiqueta()),
                            celdaCsv(e.getPrioridad().getEtiqueta()),
                            celdaCsv(e.getFechaRegistro().toLocalDate().toString())))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String celdaCsv(String valor) {
        String v = valor == null ? "" : valor;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private List<Envio> listarEnviosParaExportacion(FiltroEnvioDTO filtro) {
        return envioRepository.findAll(construirSpecification(filtro), Sort.by(Sort.Direction.DESC, "fechaRegistro"));
    }

    // ------------------------------------------------------------------ filtros y orden

    private Specification<Envio> construirSpecification(FiltroEnvioDTO filtro) {
        final String texto = filtro.getTextoBusqueda() == null ? "" : filtro.getTextoBusqueda().trim().toLowerCase();
        final EstadoEnvio estado = parseEstadoFiltro(filtro.getEstado());
        final TipoPrioridad tipo = parseTipoFiltro(filtro.getTipo());
        final LocalDateTime desde = parseFechaFiltro(filtro.getFechaInicio(), false);
        final LocalDateTime hasta = parseFechaFiltro(filtro.getFechaFin(), true);
        // Un fin dado solo como fecha (yyyy-MM-dd) cubre el día completo: se compara contra el inicio del día siguiente.
        final boolean hastaExclusivo = hasta != null && filtro.getFechaFin().trim().length() == 10;

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!texto.isEmpty()) {
                String patron = "%" + texto + "%";
                String patronSinGuiones = "%" + texto.replace("-", "") + "%";
                Predicate porTracking = cb.like(cb.lower(root.get("trackingNumber")), patron);
                // permite buscar el código sin guiones (LOG384729155)
                Predicate porTrackingSinGuiones = cb.like(
                        cb.lower(cb.function("replace", String.class, root.get("trackingNumber"), cb.literal("-"), cb.literal(""))),
                        patronSinGuiones);
                Predicate porRemitente = cb.like(cb.lower(root.get("remitente").get("nombre")), patron);
                Predicate porDestinatario = cb.like(cb.lower(root.get("destinatario").get("nombre")), patron);
                predicates.add(cb.or(porTracking, porTrackingSinGuiones, porRemitente, porDestinatario));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estadoOperativo"), estado));
            } else {
                // Los envíos eliminados (CANCELADO) no aparecen salvo que se pida ese estado explícitamente.
                predicates.add(cb.notEqual(root.get("estadoOperativo"), EstadoEnvio.CANCELADO));
            }
            if (tipo != null) {
                predicates.add(cb.equal(root.get("prioridad"), tipo));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde));
            }
            if (hasta != null) {
                predicates.add(hastaExclusivo
                        ? cb.lessThan(root.get("fechaRegistro"), hasta)
                        : cb.lessThanOrEqualTo(root.get("fechaRegistro"), hasta));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable normalizarPaginacion(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        }
        List<Sort.Order> ordenes = new ArrayList<>();
        for (Sort.Order o : pageable.getSort()) {
            String propiedad = ALIAS_ORDEN.getOrDefault(o.getProperty(), o.getProperty());
            if (!CAMPOS_ORDEN.contains(propiedad)) {
                throw new BusinessRuleException("Campo de ordenamiento no válido: " + o.getProperty());
            }
            ordenes.add(o.withProperty(propiedad));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(ordenes));
    }

    private EstadoEnvio parseEstadoFiltro(String valor) {
        try {
            return EstadoEnvio.fromTexto(valor);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(e.getMessage());
        }
    }

    private TipoPrioridad parseTipoFiltro(String valor) {
        try {
            return TipoPrioridad.fromTexto(valor);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(e.getMessage());
        }
    }

    private LocalDateTime parseFechaFiltro(String valor, boolean esFin) {
        if (valor == null || valor.isBlank()) return null;
        String v = valor.trim();
        try {
            if (v.length() == 10) {
                LocalDate dia = LocalDate.parse(v);
                return esFin ? dia.plusDays(1).atStartOfDay() : dia.atStartOfDay();
            }
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("Fecha de filtro no válida (use yyyy-MM-dd o yyyy-MM-dd'T'HH:mm:ss): " + valor);
        }
    }

    // ------------------------------------------------------------------ mapeo a DTO

    private EnvioDetalleDTO mapToDTO(Envio envio, boolean conAuditoria) {
        List<MovimientoEnvio> movimientos = movimientoRepository.findByEnvioIdOrderByFechaHoraAsc(envio.getId());
        String ubicacionActual = movimientos.stream()
                .map(MovimientoEnvio::getUbicacion)
                .filter(u -> u != null && !u.isBlank())
                .reduce((primero, segundo) -> segundo).orElse(null);
        Double[] dims = DimensionesUtil.parsear(envio.getDimensiones());

        return EnvioDetalleDTO.builder()
                .id(envio.getId())
                .trackingNumber(envio.getTrackingNumber())
                .prioridad(envio.getPrioridad())
                .prioridadEtiqueta(envio.getPrioridad().getEtiqueta())
                .peso(envio.getPeso())
                .dimensiones(envio.getDimensiones())
                .largo(dims == null ? null : dims[0])
                .ancho(dims == null ? null : dims[1])
                .alto(dims == null ? null : dims[2])
                .costo(envio.getCosto())
                .fechaRegistro(envio.getFechaRegistro())
                .fechaEstimadaEntrega(envio.getFechaEstimadaEntrega())
                .estadoOperativo(envio.getEstadoOperativo())
                .estadoEtiqueta(envio.getEstadoOperativo().getEtiqueta())
                .idRemitente(envio.getRemitente().getId())
                .nombreRemitente(envio.getRemitente().getNombre())
                .telefonoRemitente(envio.getRemitente().getTelefono())
                .emailRemitente(envio.getRemitente().getEmail())
                .direccionOrigen(DireccionDTO.from(envio.getDireccionOrigen()))
                .idDestinatario(envio.getDestinatario().getId())
                .nombreDestinatario(envio.getDestinatario().getNombre())
                .telefonoDestinatario(envio.getDestinatario().getTelefono())
                .emailDestinatario(envio.getDestinatario().getEmail())
                .direccionDestino(DireccionDTO.from(envio.getDireccionDestino()))
                .historialMovimientos(movimientos.stream().map(this::mapMovimiento).toList())
                .ubicacionActual(ubicacionActual)
                .auditoria(conAuditoria
                        ? auditoriaRepository.findByEnvioIdOrderByFechaHoraDesc(envio.getId()).stream().map(this::mapAuditoria).toList()
                        : null)
                .build();
    }

    private MovimientoEnvioDTO mapMovimiento(MovimientoEnvio movimiento) {
        return MovimientoEnvioDTO.builder().id(movimiento.getId()).estado(movimiento.getEstado())
                .estadoEtiqueta(movimiento.getEstado().getEtiqueta())
                .descripcion(movimiento.getDescripcion()).ubicacion(movimiento.getUbicacion())
                .fechaHora(movimiento.getFechaHora()).build();
    }

    private AuditoriaDTO mapAuditoria(RegistroAuditoria r) {
        List<String> campos = r.getCamposModificados() == null ? List.of()
                : Arrays.stream(r.getCamposModificados().split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return AuditoriaDTO.builder().id(r.getId()).usuario(r.getUsuarioModificador())
                .campos(campos).fechaHora(r.getFechaHora()).build();
    }

    // ------------------------------------------------------------------ movimientos (helpers)

    private MovimientoEnvio guardarMovimiento(Envio envio, EstadoEnvio estado, String descripcion, String ubicacion) {
        return movimientoRepository.save(MovimientoEnvio.builder().envio(envio).estado(estado)
                .descripcion(descripcion).ubicacion(ubicacion).build());
    }

    private String ubicacionActual(Long envioId) {
        return movimientoRepository.findByEnvioIdOrderByFechaHoraAsc(envioId).stream()
                .map(MovimientoEnvio::getUbicacion)
                .filter(u -> u != null && !u.isBlank())
                .reduce((primero, segundo) -> segundo).orElse(null);
    }

    // ------------------------------------------------------------------ clientes

    private Envio buscar(Long id) {
        return envioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado"));
    }

    private Cliente getCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private Cliente resolveCliente(CrearEnvioDTO.ClienteEnvioDTO data, Long legacyId, String role) {
        if (data == null) {
            if (legacyId == null) {
                throw new BusinessRuleException("Debe informar los datos del " + role + " o su ID");
            }
            return getCliente(legacyId);
        }
        String email = data.getEmail().trim();
        Cliente cliente = clienteRepository.findFirstByEmailIgnoreCase(email).orElse(null);
        if (cliente != null) {
            if (!cliente.getNombre().equalsIgnoreCase(data.getNombre().trim())
                    || !Objects.equals(cliente.getTelefono(), data.getTelefono().trim())) {
                throw new BusinessRuleException("El email del " + role + " ya pertenece a otro cliente");
            }
            return cliente;
        }
        return clienteRepository.save(Cliente.builder().nombre(data.getNombre().trim())
                .telefono(data.getTelefono().trim()).email(email).build());
    }

    /**
     * Aplica los datos de contacto editados desde el front. Los campos no informados se conservan.
     * Si cambia el correo se busca/crea el cliente de ese correo (no se pisa a otra persona);
     * si el correo es el mismo, se corrige nombre/teléfono del cliente existente.
     */
    private Cliente actualizarCliente(Cliente actual, ActualizarEnvioDTO.ClienteActualizarDTO d, String rol, List<String> cambios) {
        String nombre = valorOActual(d.getNombre(), actual.getNombre());
        String telefono = valorOActual(d.getTelefono(), actual.getTelefono());
        String email = valorOActual(d.getEmail(), actual.getEmail());
        if (email == null) {
            throw new BusinessRuleException("El email del " + rol + " es obligatorio");
        }

        boolean cambioNombre = !Objects.equals(actual.getNombre(), nombre);
        boolean cambioTelefono = !Objects.equals(actual.getTelefono(), telefono);
        boolean cambioEmail = actual.getEmail() == null || !actual.getEmail().equalsIgnoreCase(email);
        if (!cambioNombre && !cambioTelefono && !cambioEmail) return actual;

        if (cambioNombre) cambios.add("Nombre " + rol + " modificado: " + actual.getNombre() + " -> " + nombre);
        if (cambioTelefono) cambios.add("Teléfono " + rol + " modificado: " + valorOVacio(actual.getTelefono()) + " -> " + telefono);
        if (cambioEmail) cambios.add("Correo " + rol + " modificado: " + valorOVacio(actual.getEmail()) + " -> " + email);

        if (!cambioEmail) {
            actual.setNombre(nombre);
            actual.setTelefono(telefono);
            return clienteRepository.save(actual);
        }
        Cliente existente = clienteRepository.findFirstByEmailIgnoreCase(email).orElse(null);
        if (existente != null) {
            if (!existente.getNombre().equalsIgnoreCase(nombre) || !Objects.equals(existente.getTelefono(), telefono)) {
                throw new BusinessRuleException("El email del " + rol + " ya pertenece a otro cliente");
            }
            return existente;
        }
        return clienteRepository.save(Cliente.builder().nombre(nombre).telefono(telefono).email(email).build());
    }

    // ------------------------------------------------------------------ direcciones

    private Direccion getDireccion(Long id) {
        return direccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada: " + id));
    }

    private Direccion resolveDireccion(CrearEnvioDTO.DireccionEnvioDTO data, Long legacyId, String role) {
        if (data == null) {
            if (legacyId == null) {
                throw new BusinessRuleException("Debe informar la dirección de " + role + " o su ID");
            }
            return getDireccion(legacyId);
        }
        return buscarOCrearDireccion(data.getCalle().trim(), data.getNumero().trim(), data.getCiudad().trim(),
                normalize(data.getDepartamentoProvincia()), normalize(data.getCodigoPostal()), normalize(data.getPais()));
    }

    /** La dirección de origen no es obligatoria (el formulario del front solo pide la de destino). */
    private Direccion resolveDireccionOpcional(CrearEnvioDTO.DireccionEnvioDTO data, Long legacyId) {
        if (data == null && legacyId == null) return null;
        return resolveDireccion(data, legacyId, "origen");
    }

    /** Nunca se modifica una dirección existente (puede compartirse entre envíos): se busca una igual o se crea otra. */
    private Direccion actualizarDireccion(Direccion actual, ActualizarEnvioDTO.DireccionActualizarDTO d, String rol, List<String> cambios) {
        String calle = valorOActual(d.getCalle(), actual == null ? null : actual.getCalle());
        String numero = valorOActual(d.getNumero(), actual == null ? null : actual.getNumero());
        String ciudad = valorOActual(d.getCiudad(), actual == null ? null : actual.getCiudad());
        String depto = d.getDepartamentoProvincia() != null ? normalize(d.getDepartamentoProvincia())
                : (actual == null ? null : actual.getDepartamentoProvincia());
        String postal = d.getCodigoPostal() != null ? normalize(d.getCodigoPostal())
                : (actual == null ? null : actual.getCodigoPostal());
        String pais = d.getPais() != null ? normalize(d.getPais()) : (actual == null ? null : actual.getPais());
        if (calle == null || numero == null || ciudad == null) {
            throw new BusinessRuleException("La calle, el número y la ciudad de la dirección de " + rol + " son obligatorios");
        }
        Direccion nueva = buscarOCrearDireccion(calle, numero, ciudad, depto, postal, pais);
        if (actual == null || !Objects.equals(actual.getId(), nueva.getId())) {
            cambios.add("Dirección de " + rol + " modificada");
        }
        return nueva;
    }

    private Direccion buscarOCrearDireccion(String calle, String numero, String ciudad,
                                            String departamento, String postal, String pais) {
        for (Direccion d : direccionRepository.findByCalleIgnoreCaseAndNumeroIgnoreCaseAndCiudadIgnoreCase(calle, numero, ciudad)) {
            if (igualIgnorandoMayusculas(d.getDepartamentoProvincia(), departamento)
                    && igualIgnorandoMayusculas(d.getCodigoPostal(), postal)
                    && igualIgnorandoMayusculas(d.getPais(), pais)) {
                return d;
            }
        }
        return direccionRepository.save(Direccion.builder().calle(calle).numero(numero).ciudad(ciudad)
                .departamentoProvincia(departamento).codigoPostal(postal).pais(pais).build());
    }

    // ------------------------------------------------------------------ dimensiones y fechas

    private String dimensionesParaCrear(CrearEnvioDTO dto) {
        boolean alguna = dto.getLargo() != null || dto.getAncho() != null || dto.getAlto() != null;
        if (alguna) {
            if (dto.getLargo() == null || dto.getAncho() == null || dto.getAlto() == null) {
                throw new BusinessRuleException("Debe informar largo, ancho y alto");
            }
            return DimensionesUtil.formatear(dto.getLargo(), dto.getAncho(), dto.getAlto());
        }
        return normalize(dto.getDimensiones());
    }

    /** @return las dimensiones resultantes, o null si la petición no toca las dimensiones. */
    private String dimensionesParaActualizar(ActualizarEnvioDTO dto, String actuales) {
        boolean alguna = dto.getLargo() != null || dto.getAncho() != null || dto.getAlto() != null;
        if (alguna) {
            Double[] base = DimensionesUtil.parsear(actuales);
            Double largo = dto.getLargo() != null ? dto.getLargo() : (base == null ? null : base[0]);
            Double ancho = dto.getAncho() != null ? dto.getAncho() : (base == null ? null : base[1]);
            Double alto = dto.getAlto() != null ? dto.getAlto() : (base == null ? null : base[2]);
            if (largo == null || ancho == null || alto == null) {
                throw new BusinessRuleException("Debe informar largo, ancho y alto");
            }
            return DimensionesUtil.formatear(largo, ancho, alto);
        }
        return normalize(dto.getDimensiones());
    }

    private LocalDateTime parseFechaEstimada(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new BusinessRuleException("La fecha estimada debe usar formato ISO (yyyy-MM-dd o yyyy-MM-dd'T'HH:mm:ss)");
            }
        }
    }

    private void validarFechaProgramada(LocalDateTime fecha) {
        if (fecha.toLocalDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("La fecha programada no puede ser anterior a hoy");
        }
    }

    // ------------------------------------------------------------------ varios

    private PersonalLogistico getOperador(Long id) {
        return personalLogisticoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personal logístico no encontrado: " + id));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String valorOActual(String nuevo, String actual) {
        return nuevo == null || nuevo.isBlank() ? actual : nuevo.trim();
    }

    private String valorOVacio(String valor) {
        return valor == null ? "(vacío)" : valor;
    }

    private boolean igualIgnorandoMayusculas(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
