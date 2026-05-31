package com.videsol.backend.controller;

import com.videsol.backend.dto.response.PaginaDTO;
import com.videsol.backend.dto.response.VehiculoOkmCardDTO;
import com.videsol.backend.dto.response.VehiculoOkmDTO;
import com.videsol.backend.service.VehiculoOkmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/okm")
@RequiredArgsConstructor
@Tag(name = "0KM público", description = "Vehículos 0KM activos para mostrar en la web")
public class OkmController {

    private final VehiculoOkmService service;

    @GetMapping
    @Operation(summary = "Listar vehículos 0KM activos con filtros opcionales")
    public ResponseEntity<PaginaDTO<VehiculoOkmCardDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String combustible,
            @RequestParam(required = false) String transmision,
            @RequestParam(required = false) BigDecimal precioMax) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                service.listarActivosFiltrado(pageable, marca, tipo, combustible, transmision, precioMax)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un vehículo 0KM por ID con detalle completo")
    public ResponseEntity<VehiculoOkmDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }
}