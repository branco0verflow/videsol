package com.videsol.backend.controller;

import com.videsol.backend.dto.pilot.PilotStockDTO;
import com.videsol.backend.dto.pilot.PilotStockListResponse;
import com.videsol.backend.dto.request.AdminRequest;
import com.videsol.backend.dto.request.CambiarPasswordRequest;
import com.videsol.backend.dto.request.VehiculoOkmRequest;
import com.videsol.backend.dto.request.VehiculoUsadoRequest;
import com.videsol.backend.dto.response.AdminDTO;
import com.videsol.backend.dto.response.PendienteDTO;
import com.videsol.backend.dto.response.VehiculoOkmDTO;
import com.videsol.backend.dto.response.VehiculoUsadoDTO;
import com.videsol.backend.entity.Administrador;
import com.videsol.backend.repository.VehiculoOkmRepository;
import com.videsol.backend.repository.VehiculoUsadoRepository;
import com.videsol.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Endpoints administrativos.
 * SIN AUTENTICACIÓN POR AHORA. Agregar JWT antes de producción.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Gestión interna - sin auth por ahora")
public class AdminController {

    private final AdminOkmService adminOkmService;
    private final VehiculoUsadoService usadoService;
    private final SincronizacionService sincronizacionService;
    private final PilotStockService stockService;
    private final VehiculoOkmRepository okmRepository;
    private final VehiculoUsadoRepository usadoRepository;
    private final VehiculoOkmService vehiculoOkmService;
    private final VehiculoUsadoService vehiculoUsadoService;
    private final AdminAuthService authService;

    // ============== Sincronización / Pilot ==============

    @GetMapping("/pendientes")
    @Operation(summary = "0KM en Pilot sin completar en la DB local")
    public ResponseEntity<List<PendienteDTO>> pendientes() {
        return ResponseEntity.ok(sincronizacionService.listarPendientes());
    }

    @GetMapping("/pilot/precios")
    @Operation(summary = "Buscar en lista de precios de Pilot por marca o modelo (0KM)")
    public ResponseEntity<List<PendienteDTO>> buscarEnPilot(
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String modelo) {
        return ResponseEntity.ok(sincronizacionService.buscarEnPilot(marca, modelo));
    }

    @GetMapping("/inconsistencias")
    @Operation(summary = "0KM en tu DB que ya no existen en Pilot")
    public ResponseEntity<List<PendienteDTO>> inconsistencias() {
        return ResponseEntity.ok(sincronizacionService.detectarInconsistencias());
    }

    /**
     * Lista de usados disponibles en Pilot (para que el admin elija cuáles cargar).
     * Paginado directamente desde Pilot.
     */
    @GetMapping("/pilot/stock")
    @Operation(summary = "Listar usados disponibles en Pilot con su estado en tu DB")
    public ResponseEntity<List<Map<String, Object>>> stockPilot(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        PilotStockListResponse response = stockService.listarUsadosDisponibles(page, limit);

        List<Map<String, Object>> resultado = response.result().entitydata().stream()
                .map(s -> {
                    boolean existeEnDB = usadoRepository.existsByPilotId(s.id());
                    String estado = existeEnDB
                            ? usadoRepository.findByPilotId(s.id())
                            .map(v -> Boolean.TRUE.equals(v.getActivo()) ? "ACTIVO" : "INACTIVO")
                            .orElse("NO_CARGADO")
                            : "NO_CARGADO";

                    return Map.<String, Object>of(
                            "pilotId",    s.id(),
                            "marca",      s.brand() != null ? s.brand() : "",
                            "modelo",     s.model() != null ? s.model() : "",
                            "version",    s.version() != null ? s.version() : "",
                            "anio",       s.year() != null ? s.year() : "",
                            "km",         s.odometer() != null ? s.odometer() : "",
                            "combustible",s.fuel() != null ? s.fuel().name() : "",
                            "color",      s.color() != null ? s.color() : "",
                            "precio",     s.getSalePrice() != null ? s.getSalePrice() : "",
                            "estado",     estado
                    );
                })
                .toList();

        return ResponseEntity.ok(resultado);
    }

    // ============== 0KM ==============

    @GetMapping("/okm")
    @Operation(summary = "Listar todos los 0KM incluyendo inactivos")
    public ResponseEntity<List<VehiculoOkmDTO>> listarOkm() {
        return ResponseEntity.ok(adminOkmService.listarTodos());
    }

    @PostMapping("/okm")
    @Operation(summary = "Crear vehículo 0KM")
    public ResponseEntity<VehiculoOkmDTO> crearOkm(@Valid @RequestBody VehiculoOkmRequest req) {
        return ResponseEntity.ok(adminOkmService.crear(req));
    }

    @PutMapping("/okm/{id}")
    @Operation(summary = "Actualizar vehículo 0KM")
    public ResponseEntity<VehiculoOkmDTO> actualizarOkm(@PathVariable Long id,
                                                        @Valid @RequestBody VehiculoOkmRequest req) {
        return ResponseEntity.ok(adminOkmService.actualizar(id, req));
    }

    @PatchMapping("/okm/{id}/activar")
    @Operation(summary = "Activar o desactivar 0KM por ID")
    public ResponseEntity<VehiculoOkmDTO> activarOkm(@PathVariable Long id,
                                                     @RequestParam boolean activo) {
        return ResponseEntity.ok(adminOkmService.cambiarActivo(id, activo));
    }

    @PatchMapping("/okm/code/{code}/activar")
    @Operation(summary = "Activar o desactivar 0KM por code de Pilot")
    public ResponseEntity<Map<String, Object>> activarOkmPorCode(
            @PathVariable String code,
            @RequestParam boolean activo) {

        return okmRepository.findByCode(code)
                .map(v -> {
                    v.setActivo(activo);
                    okmRepository.save(v);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "code", code,
                            "activo", activo,
                            "mensaje", activo ? "Publicado en la web" : "Quitado de la web"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/okm/{id}")
    @Operation(summary = "Eliminar 0KM de la DB local por ID")
    public ResponseEntity<Void> eliminarOkm(@PathVariable Long id) {
        adminOkmService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/okm/code/{code}")
    @Operation(summary = "Eliminar 0KM de la DB local por code de Pilot")
    public ResponseEntity<Void> eliminarOkmPorCode(@PathVariable String code) {
        okmRepository.findByCode(code)
                .ifPresentOrElse(
                        v -> okmRepository.delete(v),
                        () -> { throw new com.videsol.backend.exception.ResourceNotFoundException(
                                "No existe vehículo con code " + code); }
                );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/okm/{id}")
    @Operation(summary = "Obtener 0KM por ID (admin)")
    public ResponseEntity<VehiculoOkmDTO> obtenerOkmAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(vehiculoOkmService.obtenerPorIdAdmin(id));
    }

    // ============== Usados ==============

    @GetMapping("/usados")
    @Operation(summary = "Listar todos los usados incluyendo inactivos")
    public ResponseEntity<List<VehiculoUsadoDTO>> listarUsados() {
        return ResponseEntity.ok(usadoService.listarTodosAdmin());
    }

    @PostMapping("/usados")
    @Operation(summary = "Crear usado")
    public ResponseEntity<VehiculoUsadoDTO> crearUsado(@Valid @RequestBody VehiculoUsadoRequest req) {
        return ResponseEntity.ok(usadoService.crear(req));
    }

    @PutMapping("/usados/{id}")
    @Operation(summary = "Actualizar usado")
    public ResponseEntity<VehiculoUsadoDTO> actualizarUsado(@PathVariable Long id,
                                                            @Valid @RequestBody VehiculoUsadoRequest req) {
        return ResponseEntity.ok(usadoService.actualizar(id, req));
    }

    @PatchMapping("/usados/pilot/{pilotId}/activar")
    @Operation(summary = "Activar o desactivar usado por pilotId")
    public ResponseEntity<VehiculoUsadoDTO> activarUsadoPorPilotId(
            @PathVariable String pilotId,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(usadoService.cambiarActivoPorPilotId(pilotId, activo));
    }

    @DeleteMapping("/usados/{id}")
    @Operation(summary = "Eliminar usado por ID")
    public ResponseEntity<Void> eliminarUsado(@PathVariable Long id) {
        usadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/usados/pilot/{pilotId}")
    @Operation(summary = "Eliminar usado por pilotId de Pilot")
    public ResponseEntity<Void> eliminarUsadoPorPilotId(@PathVariable String pilotId) {
        usadoService.eliminarPorPilotId(pilotId);
        return ResponseEntity.noContent().build();
    }

    // Inconsistencia para los vehículos que ya están vendidos pero permanecen en la DB (hay que borrarlos)

    @GetMapping("/usados/inconsistencias")
    @Operation(summary = "Usados en tu DB que ya no están disponibles en Pilot")
    public ResponseEntity<List<Map<String, Object>>> inconsistenciasUsados() {
        // Traemos todos los pilotIds disponibles en Pilot
        List<PilotStockDTO> disponibles = stockService.traerTodosDisponibles();
        Set<String> pilotIdsDisponibles = disponibles.stream()
                .map(PilotStockDTO::id)
                .collect(java.util.stream.Collectors.toSet());

        // Buscamos los que están en tu DB pero ya no están disponibles en Pilot
        return ResponseEntity.ok(
                usadoRepository.findAll().stream()
                        .filter(v -> !pilotIdsDisponibles.contains(v.getPilotId()))
                        .map(v -> Map.<String, Object>of(
                                "id",       v.getId(),
                                "pilotId",  v.getPilotId(),
                                "marca",    v.getMarca() != null ? v.getMarca() : "",
                                "modelo",   v.getModelo() != null ? v.getModelo() : "",
                                "version",  v.getVersion() != null ? v.getVersion() : "",
                                "activo",   v.getActivo(),
                                "motivo",   "VENDIDO_O_NO_DISPONIBLE"
                        ))
                        .collect(java.util.stream.Collectors.toList())
        );
    }

    @GetMapping("/usados/{id}")
    @Operation(summary = "Obtener un usado por ID con detalle completo y precio fresco de Pilot")
    public ResponseEntity<VehiculoUsadoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(vehiculoUsadoService.obtenerPorId(id));
    }


    // ============== Gestión de admins (solo super admin) ==============

    @GetMapping("/administradores")
    @Operation(summary = "Listar todos los administradores")
    public ResponseEntity<List<AdminDTO>> listarAdmins() {
        return ResponseEntity.ok(authService.listarAdmins());
    }

    @PostMapping("/administradores")
    @Operation(summary = "Crear nuevo administrador")
    public ResponseEntity<AdminDTO> crearAdmin(
            @Valid @RequestBody AdminRequest req) {
        Administrador nuevo = authService.crearAdmin(
                req.nombreAdmin(), req.email(), req.password(), req.rolSuper());
        return ResponseEntity.ok(new AdminDTO(
                nuevo.getId(), nuevo.getNombreAdmin(),
                nuevo.getEmail(), nuevo.getRolSuper(), nuevo.getActivo()));
    }

    @PutMapping("/administradores/{id}")
    @Operation(summary = "Editar administrador")
    public ResponseEntity<AdminDTO> editarAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequest req) {
        return ResponseEntity.ok(authService.editarAdmin(
                id, req.nombreAdmin(), req.email(), req.rolSuper(), true));
    }

    @PatchMapping("/administradores/{id}/password")
    @Operation(summary = "Cambiar contraseña de un administrador")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable Long id,
            @RequestBody CambiarPasswordRequest req) {
        authService.cambiarPassword(id, req.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/administradores/{id}")
    @Operation(summary = "Eliminar administrador")
    public ResponseEntity<Void> eliminarAdmin(@PathVariable Long id) {
        authService.eliminarAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
