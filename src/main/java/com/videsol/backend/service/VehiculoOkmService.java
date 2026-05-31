package com.videsol.backend.service;

import com.videsol.backend.dto.response.PaginaDTO;
import com.videsol.backend.dto.response.PrecioDTO;
import com.videsol.backend.dto.response.VehiculoOkmCardDTO;
import com.videsol.backend.dto.response.VehiculoOkmDTO;
import com.videsol.backend.entity.CaracteristicaOkm;
import com.videsol.backend.entity.ColorOkm;
import com.videsol.backend.entity.VehiculoOkm;
import com.videsol.backend.exception.ResourceNotFoundException;
import com.videsol.backend.repository.VehiculoOkmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Combina la info de tu DB con el precio fresco de Pilot.
 * Solo expone vehículos activos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehiculoOkmService {

    private final VehiculoOkmRepository repository;
    private final PilotPriceListService precioService;

    /**
     * Lista todos los vehículos 0KM activos para la web pública.
     */
    @Transactional(readOnly = true)
    public List<VehiculoOkmDTO> listarActivos() {
        return repository.findByActivoTrue().stream()
                .map(this::ensamblarConPrecio)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un vehículo 0KM por su ID interno de DB.
     */
    public VehiculoOkmDTO obtenerPorId(Long id) {
        VehiculoOkm v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo 0KM con id " + id + " no encontrado"));

        if (!Boolean.TRUE.equals(v.getActivo())) {
            throw new ResourceNotFoundException("Vehículo 0KM con id " + id + " no está activo");
        }

        PrecioDTO precio = obtenerPrecioSeguro(v.getCode());

        // Si Pilot no tiene el vehículo, lo tratamos como no encontrado
        if (precio == null) {
            log.warn("Vehículo {} activo en DB pero eliminado de Pilot", v.getCode());
            throw new ResourceNotFoundException("Vehículo temporalmente no disponible");
        }

        return ensamblarConPrecio(v);
    }

    /**
     * Versión admin: trae cualquier vehículo sin importar si está activo.
     */
    @Transactional(readOnly = true)
    public VehiculoOkmDTO obtenerPorIdAdmin(Long id) {
        VehiculoOkm v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo 0KM con id " + id + " no encontrado"));
        return ensamblarConPrecio(v);
    }

    // =========================================================================
    // Mapeo: combina entidad DB + precio Pilot
    // =========================================================================

    private VehiculoOkmDTO ensamblarConPrecio(VehiculoOkm v) {
        // Intentamos obtener el precio de Pilot. Si falla, devolvemos el vehículo igual.
        PrecioDTO precio = obtenerPrecioSeguro(v.getCode());

        return new VehiculoOkmDTO(
                v.getId(),
                v.getCode(),
                precio != null ? extraerMarca(precio) : null,
                precio != null ? extraerModelo(precio) : null,
                precio != null ? precio.name() : v.getCode(),
                precio != null ? parsePrecio(precio.precio()) : null,
                precio != null ? precio.vigencia() : null,
                precio != null ? precio.tipoNegocio() : null,
                v.getActivo(),
                v.getTipo(),
                v.getAnio(),
                v.getCilindrada(),
                v.getPotencia(),
                v.getCombustible(),
                v.getPuertas(),
                v.getDireccion(),
                v.getTransmision(),
                v.getGarantia(),
                v.getFinanciacion(),
                v.getDescripcion(),
                v.getCatalogoPdfUrl(),
                mapearColores(v),
                mapearCaracteristicas(v)
        );
    }

    private PrecioDTO obtenerPrecioSeguro(String code) {
        try {
            List<PrecioDTO> precios = precioService.listarPreciosPorCodigo(code);
            return precios.isEmpty() ? null : precios.get(0);
        } catch (Exception e) {
            log.warn("No se pudo obtener precio para {}: {}", code, e.getMessage());
            return null;
        }
    }

    /**
     * El precio viene como string tipo "20990.00". Lo parseamos a BigDecimal.
     */
    private BigDecimal parsePrecio(String precio) {
        try {
            return new BigDecimal(precio);
        } catch (Exception e) {
            return null;
        }
    }

    private String extraerMarca(PrecioDTO p) {
        return p.marca();
    }

    private String extraerModelo(PrecioDTO p) {
        return p.modelo();
    }

    private List<VehiculoOkmDTO.ColorDTO> mapearColores(VehiculoOkm v) {
        if (v.getColores() == null) return List.of();
        return v.getColores().stream()
                .sorted(Comparator.comparingInt(c -> c.getOrden() == null ? 0 : c.getOrden()))
                .map(c -> new VehiculoOkmDTO.ColorDTO(
                        c.getId(),
                        c.getNombre(),
                        c.getSwatchUrl(),
                        c.getImagenPrincipalUrl(),
                        c.getImagenes() == null ? List.of() :
                                c.getImagenes().stream()
                                        .sorted(Comparator.comparingInt(i -> i.getOrden() == null ? 0 : i.getOrden()))
                                        .map(i -> new VehiculoOkmDTO.ImagenDTO(i.getId(), i.getUrl(), i.getOrden()))
                                        .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    private VehiculoOkmDTO.CaracteristicasDTO mapearCaracteristicas(VehiculoOkm v) {
        if (v.getCaracteristicas() == null) {
            return new VehiculoOkmDTO.CaracteristicasDTO(List.of(), List.of(), List.of());
        }

        List<String> seguridad = filtrar(v, CaracteristicaOkm.CategoriaCaracteristica.SEGURIDAD);
        List<String> confort = filtrar(v, CaracteristicaOkm.CategoriaCaracteristica.CONFORT);
        List<String> multimedia = filtrar(v, CaracteristicaOkm.CategoriaCaracteristica.MULTIMEDIA);

        return new VehiculoOkmDTO.CaracteristicasDTO(seguridad, confort, multimedia);
    }

    private List<String> filtrar(VehiculoOkm v, CaracteristicaOkm.CategoriaCaracteristica categoria) {
        return v.getCaracteristicas().stream()
                .filter(c -> c.getCategoria() == categoria)
                .sorted(Comparator.comparingInt(c -> c.getOrden() == null ? 0 : c.getOrden()))
                .map(CaracteristicaOkm::getNombre)
                .collect(Collectors.toList());
    }

    private VehiculoOkmCardDTO toCardDTO(VehiculoOkm v, PrecioDTO precio) {

        // Solo la primera imagen del primer color
        String imagenPrincipal = null;
        if (v.getColores() != null && !v.getColores().isEmpty()) {
            ColorOkm primerColor = v.getColores().stream()
                    .min(Comparator.comparingInt(c -> c.getOrden() == null ? 0 : c.getOrden()))
                    .orElse(null);
            if (primerColor != null) {
                imagenPrincipal = primerColor.getImagenPrincipalUrl();
            }
        }

        return new VehiculoOkmCardDTO(
                v.getId(),
                v.getCode(),
                precio != null ? precio.marca() : null,
                precio != null ? precio.modelo() : null,
                precio != null ? precio.name() : v.getCode(),
                precio != null ? parsePrecio(precio.precio()) : null,
                precio != null ? precio.tipoNegocio() : null,
                imagenPrincipal,
                v.getAnio(),
                v.getCombustible(),
                v.getTransmision(),
                v.getGarantia(),
                v.getFinanciacion()
        );
    }

    /* Para paginar los vehículos en la web: (esto trae toda la data de los vehículos, sería mejor usar CardVehiculos para el catalogo)


    @Transactional(readOnly = true)
    public PaginaDTO<VehiculoOkmDTO> listarActivosPaginado(Pageable pageable) {
        Page<VehiculoOkm> pagina = repository.findByActivoTrue(pageable);

        List<VehiculoOkmDTO> contenido = pagina.getContent().stream()
                .map(this::ensamblarConPrecio)
                .collect(Collectors.toList());

        return new PaginaDTO<>(
                contenido,
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.getSize(),
                pagina.isLast()
        );
    }*/


    @Transactional(readOnly = true)
    public PaginaDTO<VehiculoOkmCardDTO> listarActivosPaginado(Pageable pageable) {
        Page<VehiculoOkm> pagina = repository.findByActivoTrue(pageable);

        List<VehiculoOkmCardDTO> contenido = pagina.getContent().stream()
                .map(v -> toCardDTO(v, obtenerPrecioSeguro(v.getCode())))
                .collect(Collectors.toList());

        return new PaginaDTO<>(
                contenido,
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.getSize(),
                pagina.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PaginaDTO<VehiculoOkmCardDTO> listarActivosFiltrado(
            Pageable pageable,
            String marca,
            String tipo,
            String combustible,
            String transmision,
            BigDecimal precioMax) {

        // Construimos el Specification dinámicamente
        org.springframework.data.jpa.domain.Specification<VehiculoOkm> spec =
                VehiculoOkmSpec.activo();

        if (marca != null && !marca.isBlank())
            spec = spec.and(VehiculoOkmSpec.marca(marca));
        if (tipo != null && !tipo.isBlank())
            spec = spec.and(VehiculoOkmSpec.tipo(tipo));
        if (combustible != null && !combustible.isBlank())
            spec = spec.and(VehiculoOkmSpec.combustible(combustible));
        if (transmision != null && !transmision.isBlank())
            spec = spec.and(VehiculoOkmSpec.transmision(transmision));
        if (precioMax != null)
            spec = spec.and(VehiculoOkmSpec.precioMax(precioMax));

        Page<VehiculoOkm> pagina = repository.findAll(spec, pageable);

        List<VehiculoOkmCardDTO> contenido = pagina.getContent().stream()
                .map(v -> {
                    PrecioDTO precio = obtenerPrecioSeguro(v.getCode());
                    // Si Pilot no devuelve precio, el vehículo no se muestra (para evitar que se rompa si eliminan registros en la lista de precios de Pilot)
                    if (precio == null) {
                        log.warn("Vehículo {} activo en DB pero sin precio en Pilot - ocultando", v.getCode());
                        return null;
                    }
                    return toCardDTO(v, precio);
                })
                .filter(Objects::nonNull)   // ← filtra los nulls
                .collect(Collectors.toList());

        return new PaginaDTO<>(
                contenido,
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.getSize(),
                pagina.isLast()
        );
    }

}
