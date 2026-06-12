package com.videsol.backend.service;

import com.videsol.backend.config.JwtProperties;
import com.videsol.backend.entity.Administrador;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private final JwtProperties props;

    public String generarToken(Administrador admin) {
        return Jwts.builder()
                .subject(admin.getEmail())
                .claim("nombreAdmin", admin.getNombreAdmin())
                .claim("rolSuper", admin.getRolSuper())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()
                        + props.expirationHours() * 3600_000L))
                .signWith(getKey())
                .compact();
    }

    public String extraerEmail(String token) {
        return parsear(token).getSubject();
    }

    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parsear(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }
}