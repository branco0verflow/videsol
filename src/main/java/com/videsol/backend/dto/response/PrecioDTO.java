package com.videsol.backend.dto.response;

import java.util.List;

/**
 * DTO limpio de precio para el frontend.
 */
public record PrecioDTO(
        String id,
        String code,
        String name,
        String precio,
        String condicionComercial,
        String vigencia,
        String tipoNegocio,      // "0KM", "plan_ahorro", "convencional"
        boolean esPlanAhorro,
        String tipoPlanAhorro,   // ej: "70/30"
        String cuotasPlanAhorro,
        String primeraCuota,
        String descripcion,
        String marca,
        String modelo
) {
    /**
     * Respuesta completa del vehículo con sus precios.
     */
    public record VehiculoConPreciosDTO(
            // Datos del catálogo
            String id,
            String code,
            String nombre,
            String marca,        // viene de catalogo en el catálogo, o brand en el precio
            String catalogo,
            List<String> categorias,
            java.util.Map<String, java.util.Map<String, String>> caracteristicas,
            List<VehiculoDTO.ImagenDTO> imagenes,
            List<VehiculoDTO.VideoDTO> videos,

            // Precios disponibles
            List<PrecioDTO> precios
    ) {}
}
