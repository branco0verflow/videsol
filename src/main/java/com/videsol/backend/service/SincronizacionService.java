package com.videsol.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.videsol.backend.client.PilotApiClient;
import com.videsol.backend.dto.pilot.PilotPrecioDTO;
import com.videsol.backend.dto.pilot.PilotResponse;
import com.videsol.backend.dto.response.PendienteDTO;
import com.videsol.backend.entity.VehiculoOkm;
import com.videsol.backend.repository.VehiculoOkmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detecta vehículos que están en Pilot pero todavía no se cargaron
 * en tu DB con info complementaria, o que están inactivos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionService {

    private final PilotApiClient pilotApiClient;
    private final VehiculoOkmRepository repository;

    /**
     * Trae todos los precios visibles de Pilot y los compara contra tu DB.
     * Marca como NO_CARGADO los que no existen, y como INACTIVO los que existen pero activo=false.
     */
    @Transactional(readOnly = true)
    public List<PendienteDTO> listarPendientes() {
        List<PilotPrecioDTO> precios = traerTodosLosPreciosVisibles();
        Map<String, VehiculoOkm> existentes = repository.findAll().stream()
                .collect(Collectors.toMap(VehiculoOkm::getCode, v -> v, (a, b) -> a));

        return precios.stream()
                .map(p -> {
                    VehiculoOkm existente = existentes.get(p.code());
                    String estado;
                    if (existente == null) {
                        estado = "NO_CARGADO";
                    } else if (!Boolean.TRUE.equals(existente.getActivo())) {
                        estado = "INACTIVO";
                    } else {
                        return null; // Ya está activo, no es pendiente
                    }

                    String marca = (p.model() != null && p.model().brand() != null)
                            ? p.model().brand().name() : null;
                    String modelo = (p.model() != null) ? p.model().name() : null;
                    BigDecimal precio = parsePrecio(p.price());
                    String tipoNegocio = (p.businessType() != null) ? p.businessType().name() : null;

                    return new PendienteDTO(p.code(), marca, modelo, p.name(), precio, tipoNegocio, estado);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Pagina la lista de Pilot trayendo de a 100 hasta agotar.
     */
    private List<PilotPrecioDTO> traerTodosLosPreciosVisibles() {
        List<PilotPrecioDTO> todos = new ArrayList<>();
        int page = 1;
        int maxPaginas = 50; // límite duro de la API

        while (page <= maxPaginas) {
            Map<String, Object> data = Map.of(
                    "filters", List.of(
                            Map.of("field", "visible", "operation", "=", "value", "1")
                    ),
                    "limit", 100,
                    "page", page
            );

            List<PilotPrecioDTO> pagina = pilotApiClient.post(
                    "/v2/lookups/price_list/list.php",
                    "pricelist_list",
                    data,
                    new TypeReference<PilotResponse<List<PilotPrecioDTO>>>() {}
            );

            if (pagina == null || pagina.isEmpty()) break;
            todos.addAll(pagina);
            if (pagina.size() < 100) break; // última página
            page++;
        }

        log.info("Total de precios visibles en Pilot: {}", todos.size());
        return todos;
    }

    /**
     * Trae todos los precios visibles de Pilot y filtra por marca y/o modelo.
     * El admin usa esto para encontrar el vehículo que quiere activar en la web y los que están cargados pero se eliminaron de Pilot.
     */
    public List<PendienteDTO> buscarEnPilot(String marca, String modelo) {
        List<PilotPrecioDTO> todos = traerTodosLosPreciosVisibles();

        return todos.stream()
                .filter(p -> {
                    if (marca != null && !marca.isBlank()) {
                        String marcaPilot = (p.model() != null && p.model().brand() != null)
                                ? p.model().brand().name() : "";
                        if (!marcaPilot.toLowerCase().contains(marca.toLowerCase()))
                            return false;
                    }
                    if (modelo != null && !modelo.isBlank()) {
                        String modeloPilot = p.model() != null ? p.model().name() : "";
                        String versionPilot = p.name() != null ? p.name() : "";
                        if (!modeloPilot.toLowerCase().contains(modelo.toLowerCase())
                                && !versionPilot.toLowerCase().contains(modelo.toLowerCase()))
                            return false;
                    }
                    return true;
                })
                .map(p -> new PendienteDTO(
                        p.code(),
                        p.model() != null && p.model().brand() != null
                                ? p.model().brand().name() : null,
                        p.model() != null ? p.model().name() : null,
                        p.name(),
                        parsePrecio(p.price()),
                        p.businessType() != null ? p.businessType().name() : null,
                        repository.findByCode(p.code())
                                .map(v -> Boolean.TRUE.equals(v.getActivo()) ? "ACTIVO" : "INACTIVO")
                                .orElse("NO_CARGADO")
                ))
                .collect(Collectors.toList());
    }

    private BigDecimal parsePrecio(String precio) {
        try {
            return precio != null ? new BigDecimal(precio) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Esta función trae los vehículos que no tienen registros en Pilot porque fueron eliminados pero persisten en la DB
    public List<PendienteDTO> detectarInconsistencias() {
        // Traemos todos los codes que existen en Pilot
        Set<String> codesEnPilot = traerTodosLosPreciosVisibles().stream()
                .map(PilotPrecioDTO::code)
                .collect(Collectors.toSet());

        // Buscamos los que están en tu DB pero no en Pilot
        return repository.findAll().stream()
                .filter(v -> !codesEnPilot.contains(v.getCode()))
                .map(v -> new PendienteDTO(
                        v.getCode(),
                        v.getMarcaRef(),
                        null,
                        null,
                        null,
                        null,
                        "ELIMINADO_DE_PILOT"
                ))
                .collect(Collectors.toList());
    }
}
