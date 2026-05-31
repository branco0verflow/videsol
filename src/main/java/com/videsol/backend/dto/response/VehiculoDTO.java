package com.videsol.backend.dto.response;

import java.util.List;
import java.util.Map;

/**
 * DTO limpio que recibe el frontend.
 * Sin ruido interno de Pilot (deleted, audit_user, integration_code, etc.)
 */
public record VehiculoDTO(
        String id,
        String code,
        String nombre,
        String catalogo,
        List<String> categorias,

        // características agrupadas: { "Motor": { "Potencia": "155cc" }, "Ficha Técnica": { "Combustible": "Nafta" } }
        Map<String, Map<String, String>> caracteristicas,

        List<ImagenDTO> imagenes,
        List<VideoDTO> videos,
        List<String> empresas
) {
    public record ImagenDTO(
            String descripcion,
            String thumbnail,
            String phone,
            String phablet,
            String desktop
    ) {}

    public record VideoDTO(
            String descripcion,
            String uri
    ) {}
}
