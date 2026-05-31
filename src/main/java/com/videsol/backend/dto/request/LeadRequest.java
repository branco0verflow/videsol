package com.videsol.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload que envía el frontend cuando el usuario hace click en "Me interesa".
 * El backend complementa con los datos del vehículo antes de enviarlo a Pilot.
 */
public record LeadRequest(

        @NotBlank(message = "El nombre es requerido")
        String nombre,

        @NotBlank(message = "El apellido es requerido")
        String apellido,

        @NotBlank(message = "El email es requerido")
        @Email(message = "El email no es válido")
        String email,

        String telefono,

        String comentario,

        /**
         * "okm" o "usado"
         */
        @NotBlank(message = "El tipo de vehículo es requerido")
        String vehiculoTipo,

        /**
         * ID interno de tu DB (id de vehiculos_okm o vehiculos_usados)
         */
        Long vehiculoId,

        /**
         * Consentimiento de notificaciones (true = acepta)
         */
        boolean aceptaNotificaciones,

        /**
         * Consentimiento de publicidad (true = acepta)
         */
        boolean aceptaPublicidad
) {}
