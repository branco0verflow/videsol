package com.videsol.backend.dto.response;

/**
 * Respuesta al frontend después de crear un lead en Pilot.
 */
public record LeadResponseDTO(
        boolean exitoso,
        String mensaje,
        String leadId      // ID del lead en Pilot (si fue exitoso)
) {
    public static LeadResponseDTO exito(String leadId) {
        return new LeadResponseDTO(true,
                "Tu consulta fue registrada. Un asesor te contactará a la brevedad.", leadId);
    }

    public static LeadResponseDTO error(String mensaje) {
        return new LeadResponseDTO(false, mensaje, null);
    }
}
