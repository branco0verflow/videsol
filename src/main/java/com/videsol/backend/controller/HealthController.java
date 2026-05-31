package com.videsol.backend.controller;

import com.videsol.backend.service.PilotAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final PilotAuthService authService;

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "service", "videsol-backend"
        );
    }

    /**
     * Endpoint de prueba para verificar que la autenticación con Pilot funciona.
     * Devuelve solo si el token se obtuvo OK, no expone el token en sí.
     */
    @GetMapping("/pilot")
    public Map<String, Object> pilotHealth() {
        String token = authService.getToken();
        return Map.of(
                "status", "UP",
                "pilotAuth", "OK",
                "tokenPreview", token.substring(0, Math.min(20, token.length())) + "...",
                "timestamp", Instant.now().toString()
        );
    }




    @GetMapping("/token")
    public Map<String, String> getToken() {
        return Map.of("token", authService.getToken());
    }



}


