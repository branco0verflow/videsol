package com.videsol.backend.dto.response;

public record AdminDTO(
        Long id,
        String nombreAdmin,
        String email,
        Boolean rolSuper,
        Boolean activo
) {}