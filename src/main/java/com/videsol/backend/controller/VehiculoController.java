package com.videsol.backend.controller;

import com.videsol.backend.dto.response.PrecioDTO;
import com.videsol.backend.dto.response.VehiculoDTO;
import com.videsol.backend.service.PilotCatalogService;
import com.videsol.backend.service.PilotPriceListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
@Tag(name = "Vehículos", description = "Consulta de vehículos del catálogo Pilot")
public class VehiculoController {

    private final PilotCatalogService catalogService;
    private final PilotPriceListService priceListService;

    /**
     * Obtiene un vehículo por ID con sus precios incluidos.
     * GET /api/vehiculos/23
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener vehículo por ID con precios",
               description = "Busca un vehículo por ID numérico e incluye sus precios vigentes")
    public ResponseEntity<PrecioDTO.VehiculoConPreciosDTO> getVehiculoPorId(
            @Parameter(description = "ID numérico del producto en Pilot", example = "23")
            @PathVariable String id) {

        // 1. Datos del catálogo
        VehiculoDTO vehiculo = catalogService.leerProducto(id);

        // 2. Precios por el code del producto
        List<PrecioDTO> precios = obtenerPreciosSeguro(vehiculo.code());

        // 3. Ensamblamos respuesta completa
        return ResponseEntity.ok(ensamblar(vehiculo, precios));
    }

    /**
     * Busca vehículos por código con sus precios incluidos.
     * GET /api/vehiculos?code=CC3U04
     */
    @GetMapping
    @Operation(summary = "Buscar vehículos por código con precios",
               description = "Busca por código de producto e incluye precios vigentes")
    public ResponseEntity<List<PrecioDTO.VehiculoConPreciosDTO>> getVehiculosPorCodigo(
            @Parameter(description = "Código del producto en Pilot", example = "CC3U04")
            @RequestParam String code) {

        List<VehiculoDTO> vehiculos = catalogService.buscarPorCodigo(code);
        List<PrecioDTO> precios = obtenerPreciosSeguro(code);

        List<PrecioDTO.VehiculoConPreciosDTO> resultado = vehiculos.stream()
                .map(v -> ensamblar(v, precios))
                .toList();

        return ResponseEntity.ok(resultado);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Obtiene precios sin explotar si Pilot no los tiene para ese code.
     * Si falla, devuelve lista vacía y el vehículo igual se muestra.
     */
    private List<PrecioDTO> obtenerPreciosSeguro(String code) {
        try {
            return priceListService.listarPreciosPorCodigo(code);
        } catch (Exception e) {
            log.warn("No se pudieron obtener precios para code {}: {}", code, e.getMessage());
            return List.of();
        }
    }

    private PrecioDTO.VehiculoConPreciosDTO ensamblar(VehiculoDTO v, List<PrecioDTO> precios) {
        return new PrecioDTO.VehiculoConPreciosDTO(
                v.id(),
                v.code(),
                v.nombre(),
                v.catalogo(),   // marca/catálogo
                v.catalogo(),
                v.categorias(),
                v.caracteristicas(),
                v.imagenes(),
                v.videos(),
                precios
        );
    }
}
