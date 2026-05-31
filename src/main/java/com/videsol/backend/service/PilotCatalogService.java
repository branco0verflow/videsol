package com.videsol.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.videsol.backend.client.PilotApiClient;
import com.videsol.backend.dto.pilot.PilotProductoDTO;
import com.videsol.backend.dto.pilot.PilotResponse;
import com.videsol.backend.dto.response.VehiculoDTO;
import com.videsol.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PilotCatalogService {

    private final PilotApiClient pilotApiClient;

    /**
     * Lee un producto del catálogo por su ID numérico.
     * Ej: leerProducto("20005")
     */
    @Cacheable(value = "productosPorId", key = "#id")
    public VehiculoDTO leerProducto(String id) {
        log.info("Consultando catálogo Pilot por id: {}", id);

        Map<String, String> data = Map.of("id", id);

        PilotProductoDTO producto = pilotApiClient.post(
                "/v1/catalog/product/read.php",
                "product_catalog_read",
                data,
                new TypeReference<PilotResponse<PilotProductoDTO>>() {}
        );

        if (producto == null) {
            throw new ResourceNotFoundException("Producto con id " + id + " no encontrado");
        }

        return mapearVehiculo(producto);
    }

    /**
     * Busca productos por código.
     * Un mismo código puede aparecer en varios catálogos, por eso devuelve lista.
     * Ej: buscarPorCodigo("GOLGTI1")
     */
    @Cacheable(value = "productosPorCodigo", key = "#code")
    public List<VehiculoDTO> buscarPorCodigo(String code) {
        log.info("Consultando catálogo Pilot por code: {}", code);

        Map<String, String> data = Map.of("code", code);

        // Este endpoint devuelve una lista de productos
        List<PilotProductoDTO> productos = pilotApiClient.post(
                "/v1/catalog/product/list.php",
                "product_catalog_list",
                data,
                new TypeReference<PilotResponse<List<PilotProductoDTO>>>() {}
        );

        if (productos == null || productos.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron productos con code " + code);
        }

        return productos.stream()
                .map(this::mapearVehiculo)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Mapeo de Pilot → DTO limpio
    // =========================================================================

    private VehiculoDTO mapearVehiculo(PilotProductoDTO p) {
        return new VehiculoDTO(
                p.id(),
                p.code(),
                p.name(),
                p.catalog() != null ? p.catalog().name() : null,
                mapearCategorias(p.categories()),
                mapearCaracteristicas(p.characteristics()),
                mapearImagenes(p.resources()),
                mapearVideos(p.resources()),
                mapearEmpresas(p.companies())
        );
    }

    private List<String> mapearCategorias(List<PilotProductoDTO.PilotCategoryDTO> cats) {
        if (cats == null) return List.of();
        // Ordenamos por visual_order para mantener jerarquía
        return cats.stream()
                .sorted(Comparator.comparing(c -> parseOrder(c.visualOrder())))
                .map(PilotProductoDTO.PilotCategoryDTO::name)
                .collect(Collectors.toList());
    }

    /**
     * Convierte characteristics en un mapa legible:
     * { "Motor": { "Potencia": "155cc", "Descripción": "..." }, "Ficha Técnica": { "Combustible": "Nafta" } }
     */
    private Map<String, Map<String, String>> mapearCaracteristicas(
            List<PilotProductoDTO.PilotCharacteristicDTO> chars) {
        if (chars == null) return Map.of();

        // LinkedHashMap para mantener el orden visual
        Map<String, Map<String, String>> resultado = new LinkedHashMap<>();

        chars.stream()
                .sorted(Comparator.comparing(c -> parseOrder(c.visualOrder())))
                .forEach(caracteristica -> {
                    Map<String, String> atributos = new LinkedHashMap<>();
                    if (caracteristica.attributes() != null) {
                        caracteristica.attributes().stream()
                                .sorted(Comparator.comparing(a -> parseOrder(a.visualOrder())))
                                .forEach(attr -> atributos.put(attr.name(), attr.value()));
                    }
                    resultado.put(caracteristica.name(), atributos);
                });

        return resultado;
    }

    private List<VehiculoDTO.ImagenDTO> mapearImagenes(List<PilotProductoDTO.PilotResourceDTO> resources) {
        if (resources == null) return List.of();

        return resources.stream()
                .filter(r -> "image".equalsIgnoreCase(r.type()))
                .sorted(Comparator.comparing(r -> parseOrder(r.visualOrder())))
                .map(r -> {
                    // Extraemos cada tamaño del array resource[]
                    Map<String, String> uris = new HashMap<>();
                    if (r.resource() != null) {
                        r.resource().forEach(item -> {
                            if (item.size() != null && item.uri() != null) {
                                uris.put(item.size(), item.uri());
                            }
                        });
                    }
                    return new VehiculoDTO.ImagenDTO(
                            r.description(),
                            uris.get("thumbnail"),
                            uris.get("phone"),
                            uris.get("phablet"),
                            uris.get("desktop")
                    );
                })
                .collect(Collectors.toList());
    }

    private List<VehiculoDTO.VideoDTO> mapearVideos(List<PilotProductoDTO.PilotResourceDTO> resources) {
        if (resources == null) return List.of();

        return resources.stream()
                .filter(r -> "video".equalsIgnoreCase(r.type()))
                .sorted(Comparator.comparing(r -> parseOrder(r.visualOrder())))
                .map(r -> {
                    String uri = (r.resource() != null && !r.resource().isEmpty())
                            ? r.resource().get(0).uri()
                            : null;
                    return new VehiculoDTO.VideoDTO(r.description(), uri);
                })
                .collect(Collectors.toList());
    }

    private List<String> mapearEmpresas(List<PilotProductoDTO.PilotCompanyDTO> companies) {
        if (companies == null) return List.of();
        return companies.stream()
                .map(PilotProductoDTO.PilotCompanyDTO::name)
                .collect(Collectors.toList());
    }

    private int parseOrder(String visualOrder) {
        try {
            return Integer.parseInt(visualOrder);
        } catch (Exception e) {
            return 0;
        }
    }
}
