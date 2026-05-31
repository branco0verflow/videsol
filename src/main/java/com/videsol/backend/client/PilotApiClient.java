package com.videsol.backend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videsol.backend.dto.pilot.PilotRequest;
import com.videsol.backend.dto.pilot.PilotRequestHeader;
import com.videsol.backend.dto.pilot.PilotResponse;
import com.videsol.backend.exception.PilotApiException;
import com.videsol.backend.service.PilotAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Cliente HTTP de bajo nivel para llamar a los endpoints de Pilot.
 *
 * Encapsula:
 *  - Inyección automática del token (con refresh si expiró)
 *  - Construcción del header común
 *  - Reintento UNA vez si Pilot responde 401 (token vencido entre llamadas)
 *  - Mapeo de errores a PilotApiException
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PilotApiClient {

    private final WebClient pilotWebClient;
    private final PilotAuthService authService;
    private final ObjectMapper objectMapper;

    /**
     * Hace POST a un endpoint de Pilot y deserializa la respuesta.
     *
     * @param path     ruta del endpoint (ej: "/v1/catalog/product/read.php")
     * @param flowName valor de header.FlowName (ej: "product_catalog_read")
     * @param data     objeto que va en data (ej: {"id": "1"})
     * @param typeRef  tipo de la respuesta esperada en entitydata
     */
    public <T> T post(String path, String flowName, Object data, TypeReference<PilotResponse<T>> typeRef) {
        try {
            return execute(path, flowName, data, typeRef, false);
        } catch (PilotApiException e) {
            // Si el error fue de auth, refrescamos token y reintentamos UNA vez
            if (isAuthError(e)) {
                log.warn("Auth error de Pilot, refrescando token y reintentando una vez");
                authService.forceRefresh();
                return execute(path, flowName, data, typeRef, true);
            }
            throw e;
        }
    }

    private <T> T execute(String path, String flowName, Object data,
                          TypeReference<PilotResponse<T>> typeRef, boolean isRetry) {
        String token = authService.getToken();
        PilotRequest<Object> request = new PilotRequest<>(data, PilotRequestHeader.of(flowName, token));

        try {
            String rawJson = pilotWebClient.post()
                    .uri(path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Pilot response from {}: {}", path, rawJson);

            PilotResponse<T> response = objectMapper.readValue(rawJson, typeRef);

            if (response.result() == null) {
                throw new PilotApiException("empty_result", "Pilot devolvió result null");
            }
            if (!response.result().isSuccess()) {
                throw new PilotApiException(
                        response.result().code() != null ? response.result().code() : "unknown",
                        response.result().message() != null ? response.result().message() : "Pilot devolvió error"
                );
            }

            return response.result().entitydata();

        } catch (WebClientResponseException e) {
            log.error("HTTP {} from Pilot at {}: {}", e.getStatusCode(), path, e.getResponseBodyAsString());
            throw new PilotApiException("http_" + e.getStatusCode().value(), e.getMessage());
        } catch (PilotApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado llamando a Pilot {}", path, e);
            throw new PilotApiException("Error llamando a Pilot " + path, e);
        }
    }

    private boolean isAuthError(PilotApiException e) {
        String code = e.getPilotCode();
        return code != null && (code.contains("auth") || code.contains("401") || code.contains("token"));
    }
}
