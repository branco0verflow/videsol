package com.videsol.backend.dto.pilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Wrapper de la respuesta paginada del endpoint de stock de Pilot.
 * La paginación viene en aditional_data (diferente al catálogo).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PilotStockListResponse(
        String ts,
        String _id,
        PilotStockResult result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotStockResult(
            String status,
            PilotStockPagination aditional_data,
            List<PilotStockDTO> entitydata,
            String code,
            String message
    ) {
        public boolean isSuccess() {
            return "success".equalsIgnoreCase(status);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotStockPagination(
            int page,
            int page_count,
            int rows_count,
            int rows_per_page,
            int rows_in_page,
            int rows_remaining
    ) {}
}
