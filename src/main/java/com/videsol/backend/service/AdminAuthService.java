package com.videsol.backend.service;

import com.videsol.backend.dto.response.AdminDTO;
import com.videsol.backend.entity.Administrador;
import com.videsol.backend.exception.ResourceNotFoundException;
import com.videsol.backend.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    //Métodos para super admin ================================

    public List<AdminDTO> listarAdmins() {
        return repository.findAll().stream()
                .map(a -> new AdminDTO(
                        a.getId(), a.getNombreAdmin(),
                        a.getEmail(), a.getRolSuper(), a.getActivo()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public AdminDTO editarAdmin(Long id, String nombreAdmin,
                                String email, boolean rolSuper, boolean activo) {
        Administrador a = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Admin con id " + id + " no encontrado"));
        a.setNombreAdmin(nombreAdmin);
        a.setEmail(email);
        a.setRolSuper(rolSuper);
        a.setActivo(activo);
        repository.save(a);
        return new AdminDTO(a.getId(), a.getNombreAdmin(),
                a.getEmail(), a.getRolSuper(), a.getActivo());
    }

    @Transactional
    public void eliminarAdmin(Long id) {
        if (repository.count() <= 1) {
            throw new IllegalStateException(
                    "No podés eliminar el único administrador");
        }
        repository.deleteById(id);
    }

    @Transactional
    public void cambiarPassword(Long id, String passwordNueva) {
        Administrador a = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Admin con id " + id + " no encontrado"));
        a.setPasswordHash(passwordEncoder.encode(passwordNueva));
        a.setIntentosFallidos(0);
        a.setBloqueadoHasta(null);
        repository.save(a);
    }
}