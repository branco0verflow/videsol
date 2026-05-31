package com.videsol.backend.dto.response;

import java.math.BigDecimal;

/**
 * DTO liviano para mostrar en el listado de cards.
 * Solo los campos que necesita la card.
 */
public record VehiculoOkmCardDTO(
        Long id,
        String code,
        String marca,
        String modelo,
        String version,
        BigDecimal precio,
        String tipoNegocio,
        String imagenPrincipalUrl,  // solo la primera imagen del primer color
        Integer anio,
        String combustible,
        String transmision,
        Boolean garantia,
        Boolean financiacion
) {}