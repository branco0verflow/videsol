package com.videsol.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload para crear o editar un vehículo 0KM desde el panel admin.
 * El admin completa la info que Pilot no tiene.
 */
public record VehiculoOkmRequest(
        @NotBlank String code,    // debe existir en Pilot
        Boolean activo,
        String marcaRef,
        BigDecimal precioRef,
        String tipo,
        Integer anio,
        String cilindrada,
        String potencia,
        String combustible,
        Integer puertas,
        String direccion,
        String transmision,
        Boolean garantia,
        Boolean financiacion,
        String descripcion,
        String catalogoPdfUrl,
        List<ColorRequest> colores,
        CaracteristicasRequest caracteristicas
) {
    public record ColorRequest(
            String nombre,
            String swatchUrl,
            String imagenPrincipalUrl,
            Integer orden,
            List<ImagenRequest> imagenes
    ) {}

    public record ImagenRequest(
            String url,
            Integer orden
    ) {}

    public record CaracteristicasRequest(
            List<String> seguridad,
            List<String> confort,
            List<String> multimedia
    ) {}
}
