package com.videsol.backend.service;

import com.videsol.backend.entity.Administrador;
import com.videsol.backend.exception.ResourceNotFoundException;
import com.videsol.backend.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final int MAX_INTENTOS = 5;
    private static final int BLOQUEO_MINUTOS = 15;

    private final AdministradorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public String login(String email, String password) {
        Administrador admin = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        // Verificar si está bloqueado
        if (admin.getBloqueadoHasta() != null
                && LocalDateTime.now().isBefore(admin.getBloqueadoHasta())) {
            long minutos = java.time.Duration.between(
                    LocalDateTime.now(), admin.getBloqueadoHasta()).toMinutes();
            throw new IllegalStateException(
                    "Cuenta bloqueada. Intentá en " + minutos + " minutos.");
        }

        // Verificar contraseña
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            admin.setIntentosFallidos(admin.getIntentosFallidos() + 1);
            if (admin.getIntentosFallidos() >= MAX_INTENTOS) {
                admin.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS));
                admin.setIntentosFallidos(0);
                repository.save(admin);
                throw new IllegalStateException(
                        "Demasiados intentos fallidos. Cuenta bloqueada 15 minutos.");
            }
            repository.save(admin);
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(admin.getActivo())) {
            throw new IllegalStateException("Cuenta inactiva.");
        }

        // Login exitoso — resetear intentos
        admin.setIntentosFallidos(0);
        admin.setBloqueadoHasta(null);
        repository.save(admin);

        log.info("Login exitoso: {}", email);
        return jwtService.generarToken(admin);
    }

    @Transactional
    public Administrador crearAdmin(String nombreAdmin, String email,
                                    String password, boolean rolSuper) {
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un admin con ese email");
        }
        Administrador admin = new Administrador();
        admin.setNombreAdmin(nombreAdmin);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRolSuper(rolSuper);
        admin.setActivo(true);
        return repository.save(admin);
    }

    public String obtenerNombre(String email) {
        return repository.findByEmail(email)
                .map(Administrador::getNombreAdmin)
                .orElse("");
    }
}