package com.videsol.backend.controller;

import com.videsol.backend.dto.request.LeadRequest;
import com.videsol.backend.dto.response.LeadResponseDTO;
import com.videsol.backend.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Registro de consultas de interesados")
public class LeadController {

    private final LeadService leadService;

    /**
     * El usuario completó el formulario "Me interesa" y lo envió.
     * Crea el lead directamente en Pilot CRM.
     */
    @PostMapping
    @Operation(summary = "Registrar interés en un vehículo",
            description = "Crea un lead en Pilot CRM con los datos del usuario y el vehículo consultado")
    public ResponseEntity<LeadResponseDTO> crear(@Valid @RequestBody LeadRequest req) {
        LeadResponseDTO response = leadService.crearLead(req);

        // Devolvemos 200 siempre para no exponer errores internos al frontend
        // El campo 'exitoso' indica si fue bien o no
        return ResponseEntity.ok(response);
    }
}
