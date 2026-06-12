package com.videsol.backend.repository;

import com.videsol.backend.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByEmail(String email);
    Optional<Administrador> findByNombreAdmin(String nombreAdmin);
    boolean existsByEmail(String email);
}