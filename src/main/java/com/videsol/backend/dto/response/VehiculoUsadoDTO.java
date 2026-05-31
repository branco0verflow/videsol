package com.videsol.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO completo de vehículo usado para la ficha detalle.
 * Combina datos de tu DB con precio fresco de Pilot.
 */
public record VehiculoUsadoDTO(
        Long id,
        String pilotId,
        Boolean activo,

        // Datos de Pilot (guardados en DB al crear)
        String marca,
        String modelo,
        String version,
        Integer anio,
        Integer km,
        String combustible,
        String color,

        // Precio fresco de Pilot
        BigDecimal precio,
        Boolean disponibleEnPilot,

        // Datos del admin
        String tipo,
        String cilindrada,
        String potencia,
        Integer puertas,
        String direccion,
        String transmision,
        Boolean garantia,
        Boolean financiacion,
        Boolean unicoDueno,
        BigDecimal patenteMensual,
        BigDecimal patenteAnual,
        String descripcion,

        List<ImagenUsadoDTO> imagenes,

        CaracteristicasDTO caracteristicas

) {
    public record ImagenUsadoDTO(
            Long id,
            String url,
            Boolean esPrincipal,
            Integer orden
    ) {}

    // Agregar este record dentro del VehiculoUsadoDTO
    public record CaracteristicasDTO(
            List<String> seguridad,
            List<String> confort,
            List<String> multimedia
    ) {}

    /**
     * DTO liviano para el listado de cards.
     */
    public record VehiculoUsadoCardDTO(
            Long id,
            String pilotId,
            String marca,
            String modelo,
            String version,
            Integer anio,
            Integer km,
            BigDecimal precio,
            String tipo,
            String combustible,
            String transmision,
            String color,
            Boolean garantia,
            Boolean financiacion,
            Boolean unicoDueno,
            String imagenPrincipalUrl
    ) {}
}
