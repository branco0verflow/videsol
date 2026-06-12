package com.videsol.backend.controller;

import com.videsol.backend.dto.response.PaginaDTO;
import com.videsol.backend.dto.response.VehiculoUsadoDTO;
import com.videsol.backend.service.VehiculoUsadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/usados")
@RequiredArgsConstructor
@Tag(name = "Usados público", description = "Vehículos usados activos para mostrar en la web")
public class UsadoController {

    private final VehiculoUsadoService service;

    @GetMapping
    @Operation(summary = "Listar usados activos con filtros opcionales")
    public ResponseEntity<PaginaDTO<VehiculoUsadoDTO.VehiculoUsadoCardDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String combustible,
            @RequestParam(required = false) String transmision,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Integer kmMax,
            @RequestParam(required = false) Integer anioMin,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                service.listarActivosFiltrado(pageable, marca, tipo,
                        combustible, transmision, precioMax, kmMax, anioMin, sort, order)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usado por ID con detalle completo y precio fresco de Pilot")
    public ResponseEntity<VehiculoUsadoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<VehiculoUsadoDTO> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(service.obtenerPorSlug(slug));
    }
}
