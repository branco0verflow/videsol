package com.videsol.backend.dto.response;

import java.math.BigDecimal;

/**
 * Item de la lista de Pilot que aún no fue completado en tu DB.
 * Se muestra al admin para que lo "active" cargando la info complementaria.
 */
public record PendienteDTO(
        String code,
        String marca,
        String modelo,
        String version,
        BigDecimal precio,
        String tipoNegocio,
        String estado     // "NO_CARGADO" o "INACTIVO" (existe pero activo=false)
) {}
