package com.videsol.backend.repository;

import com.videsol.backend.entity.VehiculoUsado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoUsadoRepository extends JpaRepository<VehiculoUsado, Long>,
        JpaSpecificationExecutor<VehiculoUsado> {

    Optional<VehiculoUsado> findByPilotId(String pilotId);
    Optional<VehiculoUsado> findBySlug(String slug);
    boolean existsByPilotId(String pilotId);
    List<VehiculoUsado> findByActivoTrue();
    Page<VehiculoUsado> findByActivoTrue(Pageable pageable);
}
