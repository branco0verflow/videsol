package com.videsol.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.videsol.backend.client.PilotApiClient;
import com.videsol.backend.dto.pilot.PilotPrecioDTO;
import com.videsol.backend.dto.pilot.PilotResponse;
import com.videsol.backend.dto.response.PrecioDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PilotPriceListService {

    private final PilotApiClient pilotApiClient;


   @Cacheable(value = "preciosPorCodigo", key = "#productCode")
   public List<PrecioDTO> listarPreciosPorCodigo(String productCode) {
       log.info("Consultando lista de precios Pilot por code: {}", productCode);

       // LinkedHashMap garantiza el orden field → operation → value
       Map<String, Object> filtro = new LinkedHashMap<>();
       filtro.put("field", "code");
       filtro.put("operation", "=");
       filtro.put("value", productCode);

       Map<String, Object> data = new LinkedHashMap<>();
       data.put("filters", List.of(filtro));
       data.put("limit", 100);
       data.put("page", 1);

       List<PilotPrecioDTO> precios = pilotApiClient.post(
               "/v2/lookups/price_list/list.php",
               "pricelist_list",
               data,
               new TypeReference<PilotResponse<List<PilotPrecioDTO>>>() {}
       );

       if (precios == null) return List.of();

       log.info("Precios encontrados para code {}: {}", productCode, precios.size());
       return precios.stream()
               .map(this::mapearPrecio)
               .collect(Collectors.toList());
   }

    /**
     * Intento 1: filtrar directo por code del modelo + visible
     */
    private List<PrecioDTO> consultarConFiltroModelCode(String productCode) {
        var data = Map.of(
                "filters", List.of(
                        Map.of("field", "model_code", "operation", "=", "value", productCode),
                        Map.of("field", "visible", "operation", "=", "value", "1")
                ),
                "sort", List.of(Map.of("field", "name", "order", "ASC")),
                "limit", 100,
                "page", 1
        );

        List<PilotPrecioDTO> precios = pilotApiClient.post(
                "/v2/lookups/price_list/list.php",
                "pricelist_list",
                data,
                new TypeReference<PilotResponse<List<PilotPrecioDTO>>>() {}
        );

        return mapearPrecios(precios);
    }

    /**
     * Intento 2 (fallback): traer solo los visibles y filtrar en memoria por code
     * Esto es menos eficiente pero funciona si Pilot no soporta model_code como filtro
     */
    private List<PrecioDTO> consultarSoloVisible(String productCode) {
        var data = Map.of(
                "filters", List.of(
                        Map.of("field", "visible", "operation", "=", "value", "1")
                ),
                "sort", List.of(Map.of("field", "name", "order", "ASC")),
                "limit", 100,
                "page", 1
        );

        List<PilotPrecioDTO> todos = pilotApiClient.post(
                "/v2/lookups/price_list/list.php",
                "pricelist_list",
                data,
                new TypeReference<PilotResponse<List<PilotPrecioDTO>>>() {}
        );

        // Filtramos en memoria por el code del modelo
        List<PilotPrecioDTO> filtrados = todos.stream()
                .filter(p -> p.model() != null && productCode.equalsIgnoreCase(p.model().code()))
                .collect(Collectors.toList());

        log.info("Precios encontrados para code {}: {} de {} totales", productCode, filtrados.size(), todos.size());
        return mapearPrecios(filtrados);
    }

    // =========================================================================
    // Mapeo de Pilot → DTO limpio
    // =========================================================================

    private List<PrecioDTO> mapearPrecios(List<PilotPrecioDTO> pilotos) {
        if (pilotos == null) return List.of();
        return pilotos.stream()
                .map(this::mapearPrecio)
                .collect(Collectors.toList());
    }

    private PrecioDTO mapearPrecio(PilotPrecioDTO p) {
        String tipoNegocio = p.businessType() != null ? p.businessType().name() : null;
        boolean esPlanAhorro = "1".equals(p.isSavingPlan());
        String tipoPlan = (esPlanAhorro && p.savingPlanType() != null) ? p.savingPlanType().name() : null;

        // Marca y modelo vienen del model anidado
        String marca = (p.model() != null && p.model().brand() != null)
                ? p.model().brand().name() : null;
        String modelo = p.model() != null ? p.model().name() : null;

        return new PrecioDTO(
                p.id(),
                p.code(),
                p.name(),
                p.price(),
                p.commercialCondition(),
                p.validDate(),
                tipoNegocio,
                esPlanAhorro,
                tipoPlan,
                p.savingPlanShares(),
                p.savingPlanFirstShareAmount(),
                p.description(),
                marca,
                modelo

        );
    }


}
