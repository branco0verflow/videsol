package com.videsol.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.videsol.backend.config.PilotApiProperties;
import com.videsol.backend.exception.PilotApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;

/**
 * Maneja la autenticación contra Pilot API.
 *
 * El token tiene vigencia de 5hs según la documentación. Lo cacheamos en memoria
 * y lo refrescamos automáticamente antes de que expire.
 *
 * Es synchronized porque varios threads (requests concurrentes) podrían
 * intentar refrescar el token simultáneamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PilotAuthService {

    private final WebClient pilotWebClient;
    private final PilotApiProperties props;

    private volatile String cachedToken;
    private volatile Instant expiresAt;

    /**
     * Devuelve un token válido. Si el cacheado expiró o no existe, lo refresca.
     */
    public synchronized String getToken() {
        if (isTokenValid()) {
            return cachedToken;
        }
        log.info("Token de Pilot inexistente o expirado. Solicitando nuevo token...");
        return refreshToken();
    }

    /**
     * Fuerza refresh del token (útil cuando recibimos 401 en una llamada).
     */
    public synchronized String forceRefresh() {
        log.warn("Forzando refresh de token de Pilot");
        cachedToken = null;
        expiresAt = null;
        return refreshToken();
    }

    private boolean isTokenValid() {
        return cachedToken != null
                && expiresAt != null
                && Instant.now().isBefore(expiresAt);
    }

    private String refreshToken() {
        try {
            // Pilot espera username y password como query string en POST
            AuthResponse response = pilotWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/users/auth.php")
                            .queryParam("username", props.username())
                            .queryParam("password", props.password())
                            .build())
                    .retrieve()
                    .bodyToMono(AuthResponse.class)
                    .block();

            if (response == null || response.result() == null) {
                throw new PilotApiException("empty_response", "Respuesta vacía de Pilot al autenticar");
            }

            AuthResult result = response.result();
            if (!"success".equalsIgnoreCase(result.status())) {
                throw new PilotApiException(
                        result.code() != null ? result.code() : "auth_failed",
                        result.message() != null ? result.message() : "Falló autenticación con Pilot"
                );
            }

            this.cachedToken = result.entitydata();
            this.expiresAt = Instant.now().plus(Duration.ofMinutes(props.tokenTtlMinutes()));

            log.info("Token de Pilot obtenido exitosamente. Expira: {}", expiresAt);
            return cachedToken;

        } catch (WebClientResponseException e) {
            log.error("Error HTTP al autenticar con Pilot: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PilotApiException("http_error_" + e.getStatusCode().value(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al autenticar con Pilot", e);
            throw new PilotApiException("Error autenticando con Pilot", e);
        }
    }

    // === DTOs específicos del endpoint de auth ===

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthResponse(String ts, String _id, AuthResult result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthResult(
            String status,
            String entitydata,  // el token JWT viene como string acá
            String code,
            String message
    ) {}
}
