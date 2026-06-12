package com.videsol.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarPasswordRequest(
        @NotBlank String passwordActual,   // para que el admin cambie la suya
        @NotBlank String passwordNueva
) {}