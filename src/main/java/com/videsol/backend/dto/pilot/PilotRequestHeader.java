package com.videsol.backend.dto.pilot;

import java.util.List;
import java.util.UUID;

/**
 * Header común de las requests a los endpoints de catálogo de Pilot:
 * {
 *   "FlowName": "product_catalog_read",
 *   "SequenceId": [],
 *   "TimeStamp": [],
 *   "TrackingId": "uuid",
 *   "access_token": "..."
 * }
 */
public record PilotRequestHeader(
        String FlowName,
        List<Object> SequenceId,
        List<Object> TimeStamp,
        String TrackingId,
        String access_token
) {
    public static PilotRequestHeader of(String flowName, String accessToken) {
        return new PilotRequestHeader(
                flowName,
                List.of(),
                List.of(),
                UUID.randomUUID().toString().toUpperCase(),
                accessToken
        );
    }
}
