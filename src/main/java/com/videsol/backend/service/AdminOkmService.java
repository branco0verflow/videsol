package com.videsol.backend.service;

import com.videsol.backend.dto.request.VehiculoOkmRequest;
import com.videsol.backend.dto.response.VehiculoOkmDTO;
import com.videsol.backend.entity.*;
import com.videsol.backend.exception.ResourceNotFoundException;
import com.videsol.backend.repository.VehiculoOkmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica del panel admin: CRUD de la info complementaria de los 0KM.
 */
@Service
@RequiredArgsConstructor
public class AdminOkmService {

    private final VehiculoOkmRepository repository;
    private final SlugService slugService;
    private final VehiculoOkmService vehiculoOkmService;

    @Transactional(readOnly = true)
    public List<VehiculoOkmDTO> listarTodos() {
        // Admin ve todos, no solo los activos
        return repository.findAll().stream()
                .map(v -> {
                    // Reutilizamos el ensamblaje del servicio público pero forzando que muestre inactivos
                    return vehiculoOkmService.obtenerPorIdAdmin(v.getId());
                })
                .toList();
    }

    @Transactional
    public VehiculoOkmDTO crear(VehiculoOkmRequest req) {
        if (repository.existsByCode(req.code())) {
            throw new IllegalArgumentException("Ya existe un vehículo 0KM con code " + req.code());
        }
        VehiculoOkm v = new VehiculoOkm();
        v.setCode(req.code());
        String slug = slugService.generarSlugOkm(
                req.marcaRef(),
                null,   // modelo viene de Pilot, no del request
                null,   // version viene de Pilot
                req.anio()
        );
        v.setSlug(slug);
        aplicarCampos(v, req);
        VehiculoOkm guardado = repository.save(v);
        return vehiculoOkmService.obtenerPorIdAdmin(guardado.getId());
    }

    @Transactional
    public VehiculoOkmDTO actualizar(Long id, VehiculoOkmRequest req) {
        VehiculoOkm v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("0KM con id " + id + " no encontrado"));
        aplicarCampos(v, req);
        VehiculoOkm guardado = repository.save(v);
        return vehiculoOkmService.obtenerPorIdAdmin(guardado.getId());
    }

    @Transactional
    public VehiculoOkmDTO cambiarActivo(Long id, boolean activo) {
        VehiculoOkm v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("0KM con id " + id + " no encontrado"));
        v.setActivo(activo);
        repository.save(v);
        return vehiculoOkmService.obtenerPorIdAdmin(id);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("0KM con id " + id + " no encontrado");
        }
        repository.deleteById(id);
    }

    // ============== Helpers ==============

    private void aplicarCampos(VehiculoOkm v, VehiculoOkmRequest req) {
        v.setActivo(Boolean.TRUE.equals(req.activo()));
        v.setTipo(req.tipo());
        v.setAnio(req.anio());
        v.setCilindrada(req.cilindrada());
        v.setPotencia(req.potencia());
        v.setCombustible(req.combustible());
        v.setPuertas(req.puertas());
        v.setDireccion(req.direccion());
        v.setTransmision(req.transmision());
        v.setGarantia(Boolean.TRUE.equals(req.garantia()));
        v.setFinanciacion(Boolean.TRUE.equals(req.financiacion()));
        v.setDescripcion(req.descripcion());
        // Solo sobreescribe si el request trae valor — nunca pisa con null
        // (el frontend no conoce estos campos internos de filtro de Pilot)
        if (req.marcaRef() != null && !req.marcaRef().isBlank()) {
            v.setMarcaRef(req.marcaRef());
        }
        if (req.precioRef() != null) {
            v.setPrecioRef(req.precioRef());
        }
        v.setCatalogoPdfUrl(req.catalogoPdfUrl());

        // Reemplazamos colores
        v.getColores().clear();
        if (req.colores() != null) {
            req.colores().forEach(cr -> {
                ColorOkm c = new ColorOkm();
                c.setVehiculo(v);
                c.setNombre(cr.nombre());
                c.setSwatchUrl(cr.swatchUrl());
                c.setImagenPrincipalUrl(cr.imagenPrincipalUrl());
                c.setOrden(cr.orden() != null ? cr.orden() : 0);
                if (cr.imagenes() != null) {
                    cr.imagenes().forEach(ir -> {
                        ImagenOkm i = new ImagenOkm();
                        i.setColor(c);
                        i.setUrl(ir.url());
                        i.setOrden(ir.orden() != null ? ir.orden() : 0);
                        c.getImagenes().add(i);
                    });
                }
                v.getColores().add(c);
            });
        }

        // Reemplazamos características
        v.getCaracteristicas().clear();
        if (req.caracteristicas() != null) {
            agregarCaracteristicas(v, req.caracteristicas().seguridad(),
                    CaracteristicaOkm.CategoriaCaracteristica.SEGURIDAD);
            agregarCaracteristicas(v, req.caracteristicas().confort(),
                    CaracteristicaOkm.CategoriaCaracteristica.CONFORT);
            agregarCaracteristicas(v, req.caracteristicas().multimedia(),
                    CaracteristicaOkm.CategoriaCaracteristica.MULTIMEDIA);
        }
    }

    private void agregarCaracteristicas(VehiculoOkm v, List<String> items,
                                         CaracteristicaOkm.CategoriaCaracteristica categoria) {
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            CaracteristicaOkm c = new CaracteristicaOkm();
            c.setVehiculo(v);
            c.setCategoria(categoria);
            c.setNombre(items.get(i));
            c.setOrden(i);
            v.getCaracteristicas().add(c);
        }
    }
}
