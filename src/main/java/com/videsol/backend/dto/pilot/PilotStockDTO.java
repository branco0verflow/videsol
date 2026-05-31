package com.videsol.backend.dto.pilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mapea la entidad Stock tal como la devuelve Pilot (/v1/stock/list.php).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PilotStockDTO(
        String id,               // GUID — vínculo con tu DB
        @JsonProperty("integration_reference_code") String integrationReferenceCode,
        String brand,
        String model,
        String version,
        String year,
        String odometer,
        String color,
        String location,
        String accesories,
        @JsonProperty("license_plate") String licensePlate,
        String vin,
        @JsonProperty("business_channel") String businessChannel,
        @JsonProperty("published_in_web") String publishedInWeb,
        PilotStockTypeDTO type,
        List<PilotStockPriceDTO> prices,
        PilotStockFuelDTO fuel,
        @JsonProperty("availability_status") PilotStockAvailabilityDTO availabilityStatus
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotStockTypeDTO(String code, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotStockFuelDTO(String code, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotStockAvailabilityDTO(String code, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotStockPriceDTO(String type, String value) {}

    /**
     * Extrae el precio de venta (SALE_COST) de la lista de precios.
     */
    public String getSalePrice() {
        if (prices == null) return null;
        return prices.stream()
                .filter(p -> "SALE_COST".equals(p.type()))
                .map(PilotStockPriceDTO::value)
                .filter(v -> v != null && !v.isBlank() && !v.equals(".00"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Devuelve true si el vehículo está disponible (availability_status.code = "1").
     */
    public boolean isDisponible() {
        return availabilityStatus != null && "1".equals(availabilityStatus.code());
    }
}
