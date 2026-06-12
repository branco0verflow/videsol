package com.videsol.backend.controller;

import com.videsol.backend.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación de administradores")
public class AuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        try {
            String token = authService.login(
                    body.get("email"),
                    body.get("password")
            );

            // Seteamos el token en cookie httpOnly
            Cookie cookie = new Cookie("admin_token", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // true en producción (HTTPS)
            cookie.setPath("/");
            cookie.setMaxAge(8 * 3600); // 8 horas
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "nombreAdmin", authService.obtenerNombre(body.get("email"))
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(423).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("admin_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // elimina la cookie
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }
}