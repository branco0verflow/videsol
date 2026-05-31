package com.videsol.backend.dto.pilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mapea la entidad Producto tal como la devuelve Pilot.
 * Ignoramos campos que no usamos (deleted, modified, audit_user, etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PilotProductoDTO(
        String id,
        String code,
        String name,

        PilotCatalogDTO catalog,
        List<PilotCategoryDTO> categories,
        List<PilotCharacteristicDTO> characteristics,
        List<PilotResourceDTO> resources,
        List<PilotCompanyDTO> companies
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotCatalogDTO(String code, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotCompanyDTO(String code, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotCategoryDTO(
            String code,
            String name,
            @JsonProperty("visual_order") String visualOrder,
            @JsonProperty("is_leaf") String isLeaf,
            @JsonProperty("is_root") Object isRoot
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotCharacteristicDTO(
            String code,
            String name,
            @JsonProperty("visual_order") String visualOrder,
            List<PilotAttributeDTO> attributes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotAttributeDTO(
            String code,
            String name,
            String description,
            @JsonProperty("show_in_quotations") String showInQuotations,
            @JsonProperty("visual_order") String visualOrder,
            String value
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotResourceDTO(
            String type,          // "image", "video", "link"
            String description,
            @JsonProperty("visual_order") String visualOrder,
            List<PilotResourceItemDTO> resource
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotResourceItemDTO(
            String size,          // "thumbnail", "phone", "phablet", "desktop"
            String extension,
            String uri,
            @JsonProperty("mime_type") String mimeType
    ) {}
}
