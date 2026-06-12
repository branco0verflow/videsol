package com.videsol.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vehículo 0KM completo para mostrar en la web pública.
 * Mezcla datos de tu DB con precio/marca/modelo de Pilot.
 */
public record VehiculoOkmDTO(
        Long id,
        String slug,
        String code,           // referencia a Pilot
        String marca,          // viene de Pilot
        String modelo,         // viene de Pilot
        String version,        // viene de Pilot (campo name)
        BigDecimal precio,     // viene de Pilot (fresco)
        String vigenciaPrecio, // viene de Pilot
        String tipoNegocio,    // viene de Pilot

        // Lo siguiente viene de tu DB
        Boolean activo,
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
        List<ColorDTO> colores,
        CaracteristicasDTO caracteristicas
) {

    public record ColorDTO(
            Long id,
            String nombre,
            String swatchUrl,
            String imagenPrincipalUrl,
            List<ImagenDTO> imagenes
    ) {}

    public record ImagenDTO(
            Long id,
            String url,
            Integer orden
    ) {}

    public record CaracteristicasDTO(
            List<String> seguridad,
            List<String> confort,
            List<String> multimedia
    ) {}
}
