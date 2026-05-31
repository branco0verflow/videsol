package com.videsol.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload para crear o editar un vehículo usado desde el panel admin.
 *
 * Al CREAR: el admin pasa pilotId + datos de Pilot + datos propios.
 * Al EDITAR: pilotId no cambia, solo se actualizan los datos propios.
 */
public record VehiculoUsadoRequest(

        // Vínculo con Pilot — obligatorio al crear
        String pilotId,

        Boolean activo,

        // Datos de Pilot (pre-cargados del stock, el admin puede corregir)
        @NotBlank String marca,
        @NotBlank String modelo,
        String version,
        Integer anio,
        Integer km,
        String combustible,
        String color,
        BigDecimal precioRef,    // ← agregar

        // Datos que el admin completa
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

        List<ImagenRequest> imagenes,

        CaracteristicasRequest caracteristicas
) {
    public record ImagenRequest(
            String url,
            Boolean esPrincipal,
            Integer orden
    ) {}

    public record CaracteristicasRequest(
            List<String> seguridad,
            List<String> confort,
            List<String> multimedia
    ) {}

}
