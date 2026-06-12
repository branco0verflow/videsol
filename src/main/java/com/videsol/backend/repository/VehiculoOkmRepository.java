package com.videsol.backend.repository;

import com.videsol.backend.entity.VehiculoOkm;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;



@Repository
public interface VehiculoOkmRepository extends JpaRepository<VehiculoOkm, Long>,
        JpaSpecificationExecutor<VehiculoOkm> {

    Optional<VehiculoOkm> findByCode(String code);
    Optional<VehiculoOkm> findBySlug(String slug);
    boolean existsByCode(String code);
    List<VehiculoOkm> findByActivoTrue();
    Page<VehiculoOkm> findByActivoTrue(Pageable pageable);
}

