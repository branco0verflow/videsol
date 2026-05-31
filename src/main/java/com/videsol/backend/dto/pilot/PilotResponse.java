package com.videsol.backend.dto.pilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Estructura genérica que devuelven todos los endpoints de Pilot:
 * {
 *   "ts": "...",
 *   "_id": "...",
 *   "result": {
 *     "status": "success" | "error",
 *     "aditional_data": [...],
 *     "entitydata": <T> | [...],
 *     "code": "...",      // solo en error
 *     "message": "..."    // solo en error
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PilotResponse<T>(
        String ts,
        String _id,
        PilotResult<T> result
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotResult<T>(
            String status,
            T entitydata,
            String code,
            String subcode,
            String message
    ) {
        public boolean isSuccess() {
            return "success".equalsIgnoreCase(status);
        }
    }
}
