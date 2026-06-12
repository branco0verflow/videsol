package com.videsol.backend.config;

import com.videsol.backend.repository.AdministradorRepository;
import com.videsol.backend.repository.VehiculoOkmRepository;
import com.videsol.backend.repository.VehiculoUsadoRepository;
import com.videsol.backend.service.AdminAuthService;
import com.videsol.backend.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AdministradorRepository repository;
    private final AdminAuthService authService;

    private final VehiculoOkmRepository okmRepository;
    private final VehiculoUsadoRepository usadoRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public ApplicationRunner initAdmin() {
        return args -> {
            if (repository.count() == 0) {
                authService.crearAdmin(
                        "admin",
                        adminEmail,
                        adminPassword,
                        true
                );
                log.info("Super admin creado: {}", adminEmail);
                log.warn("CAMBIÁ LA CONTRASEÑA DEL ADMIN EN PRODUCCIÓN");
            }
        };
    }

    @Bean
    public ApplicationRunner migrarSlugs() {
        return args -> {
            // Migrar 0KM sin slug
            okmRepository.findAll().stream()
                    .filter(v -> v.getSlug() == null || v.getSlug().isBlank())
                    .forEach(v -> {
                        String slug = SlugService.slugify(
                                (v.getMarcaRef() != null ? v.getMarcaRef() : "vehiculo")
                                        + " " + v.getCode() + " "
                                        + (v.getAnio() != null ? v.getAnio() : ""));
                        v.setSlug(slug);
                        okmRepository.save(v);
                        log.info("Slug generado para OKM {}: {}", v.getCode(), slug);
                    });

            // Migrar usados sin slug
            usadoRepository.findAll().stream()
                    .filter(v -> v.getSlug() == null || v.getSlug().isBlank())
                    .forEach(v -> {
                        String slug = SlugService.slugify(
                                (v.getMarca() != null ? v.getMarca() : "vehiculo")
                                        + " " + (v.getModelo() != null ? v.getModelo() : "")
                                        + " " + (v.getAnio() != null ? v.getAnio() : ""));
                        v.setSlug(slug);
                        usadoRepository.save(v);
                        log.info("Slug generado para usado {}: {}", v.getPilotId(), slug);
                    });
        };
    }
}
