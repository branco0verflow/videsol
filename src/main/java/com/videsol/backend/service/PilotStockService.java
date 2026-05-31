package com.videsol.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videsol.backend.dto.pilot.PilotRequestHeader;
import com.videsol.backend.dto.pilot.PilotStockDTO;
import com.videsol.backend.dto.pilot.PilotStockListResponse;
import com.videsol.backend.exception.PilotApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Llama al endpoint de stock de Pilot (/v1/stock/list.php).
 * Filtra por type_code=VO (usados) y availability_status_code=1 (disponibles).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PilotStockService {

    private final WebClient pilotWebClient;
    private final PilotAuthService authService;
    private final ObjectMapper objectMapper;

    /**
     * Trae una página de usados disponibles de Pilot.
     */
    public PilotStockListResponse listarUsadosDisponibles(int page, int limit) {
        String token = authService.getToken();

        Map<String, Object> filtro1 = new LinkedHashMap<>();
        filtro1.put("field", "type_code");
        filtro1.put("operation", "=");
        filtro1.put("value", "VO");

        Map<String, Object> filtro2 = new LinkedHashMap<>();
        filtro2.put("field", "availability_status_code");
        filtro2.put("operation", "=");
        filtro2.put("value", "1");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filters", List.of(filtro1, filtro2));
        data.put("limit", limit);
        data.put("page", page);

        Map<String, Object> request = Map.of(
                "data", data,
                "header", Map.of(
                        "FlowName", "stock_list",
                        "SequenceId", List.of(),
                        "TimeStamp", List.of(),
                        "TrackingId", UUID.randomUUID().toString().toUpperCase(),
                        "access_token", token
                )
        );

        try {
            String rawJson = pilotWebClient.post()
                    .uri("/v1/stock/list.php")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Pilot stock response page {}: {}", page, rawJson != null ? rawJson.substring(0, Math.min(200, rawJson.length())) : "null");

            PilotStockListResponse response = objectMapper.readValue(rawJson, PilotStockListResponse.class);

            if (response.result() == null || !response.result().isSuccess()) {
                String msg = response.result() != null ? response.result().message() : "respuesta nula";
                throw new PilotApiException("stock_error", "Error al consultar stock de Pilot: " + msg);
            }

            return response;

        } catch (PilotApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llamando a stock de Pilot", e);
            throw new PilotApiException("Error consultando stock de Pilot", e);
        }
    }

    /**
     * Busca un vehículo específico en Pilot por su GUID.
     * Trae todas las páginas hasta encontrarlo (máx 10 páginas).
     */
    public Optional<PilotStockDTO> buscarPorPilotId(String pilotId) {
        for (int page = 1; page <= 10; page++) {
            PilotStockListResponse response = listarUsadosDisponibles(page, 100);
            if (response.result().entitydata() == null || response.result().entitydata().isEmpty()) break;

            Optional<PilotStockDTO> found = response.result().entitydata().stream()
                    .filter(s -> pilotId.equals(s.id()))
                    .findFirst();

            if (found.isPresent()) return found;

            if (response.result().aditional_data().rows_remaining() == 0) break;
        }
        return Optional.empty();
    }

    /**
     * Trae todos los usados disponibles paginando hasta agotar (máx 20 páginas).
     */
    public List<PilotStockDTO> traerTodosDisponibles() {
        List<PilotStockDTO> todos = new ArrayList<>();
        int page = 1;

        while (page <= 20) {
            PilotStockListResponse response = listarUsadosDisponibles(page, 100);
            List<PilotStockDTO> items = response.result().entitydata();
            if (items == null || items.isEmpty()) break;
            todos.addAll(items);
            if (response.result().aditional_data().rows_remaining() == 0) break;
            page++;
        }

        log.info("Total usados disponibles en Pilot: {}", todos.size());
        return todos;
    }
}
