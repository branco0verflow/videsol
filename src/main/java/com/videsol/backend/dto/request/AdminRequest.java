package com.videsol.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminRequest(
        @NotBlank String nombreAdmin,
        @Email @NotBlank String email,
        @NotBlank String password,
        boolean rolSuper
) {}
