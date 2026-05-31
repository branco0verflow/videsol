package com.videsol.backend.service;

import com.videsol.backend.dto.pilot.PilotStockDTO;
import com.videsol.backend.dto.request.VehiculoUsadoRequest;
import com.videsol.backend.dto.response.PaginaDTO;
import com.videsol.backend.dto.response.VehiculoUsadoDTO;
import com.videsol.backend.entity.CaracteristicaUsado;
import com.videsol.backend.entity.ImagenUsado;
import com.videsol.backend.entity.VehiculoUsado;
import com.videsol.backend.exception.ResourceNotFoundException;
import com.videsol.backend.repository.VehiculoUsadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehiculoUsadoService {

    private final VehiculoUsadoRepository repository;
    private final PilotStockService stockService;

    // ============== Público ==============

    @Transactional(readOnly = true)
    public PaginaDTO<VehiculoUsadoDTO.VehiculoUsadoCardDTO> listarActivosFiltrado(
            Pageable pageable,
            String marca,
            String tipo,
            String combustible,
            String transmision,
            BigDecimal precioMax,
            Integer kmMax,
            Integer anioMin) {

        Specification<VehiculoUsado> spec = VehiculoUsadoSpec.activo();

        if (marca != null && !marca.isBlank())
            spec = spec.and(VehiculoUsadoSpec.marca(marca));
        if (tipo != null && !tipo.isBlank())
            spec = spec.and(VehiculoUsadoSpec.tipo(tipo));
        if (combustible != null && !combustible.isBlank())
            spec = spec.and(VehiculoUsadoSpec.combustible(combustible));
        if (transmision != null && !transmision.isBlank())
            spec = spec.and(VehiculoUsadoSpec.transmision(transmision));
        if (kmMax != null)
            spec = spec.and(VehiculoUsadoSpec.kmMax(kmMax));
        if (anioMin != null)
            spec = spec.and(VehiculoUsadoSpec.anioMin(anioMin));

        Page<VehiculoUsado> pagina = repository.findAll(spec, pageable);

        // Una sola llamada a Pilot para todos — en lugar de N llamadas individuales
        Map<String, String> preciosPorPilotId = obtenerPreciosEnBatch(
                pagina.getContent().stream()
                        .map(VehiculoUsado::getPilotId)
                        .collect(Collectors.toList())
        );

        List<VehiculoUsadoDTO.VehiculoUsadoCardDTO> contenido = pagina.getContent().stream()
                .map(v -> {
                    String precioStr = preciosPorPilotId.get(v.getPilotId());

                    // Si Pilot no lo devuelve, fue vendido o ya no está disponible
                    if (precioStr == null || precioStr.isBlank()) {
                        log.warn("Usado pilotId={} no disponible en Pilot - ocultando del listado",
                                v.getPilotId());
                        return null;
                    }

                    BigDecimal precio = parsePrecio(precioStr);

                    // Filtro de precio en memoria contra precioRef de DB
                    if (precioMax != null && v.getPrecioRef() != null
                            && v.getPrecioRef().compareTo(precioMax) > 0)
                        return null;

                    return toCardDTO(v, precio);
                })
                .filter(c -> c != null)
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

    /**
     * Trae todos los usados disponibles de Pilot y devuelve mapa pilotId → precio.
     * Una sola llamada en batch en lugar de N llamadas individuales.
     */
    private Map<String, String> obtenerPreciosEnBatch(List<String> pilotIds) {
        if (pilotIds == null || pilotIds.isEmpty()) return Map.of();
        try {
            return stockService.traerTodosDisponibles().stream()
                    .filter(s -> pilotIds.contains(s.id()))
                    .collect(Collectors.toMap(
                            PilotStockDTO::id,
                            s -> s.getSalePrice() != null ? s.getSalePrice() : "",
                            (a, b) -> a
                    ));
        } catch (Exception e) {
            log.error("Error obteniendo precios en batch de Pilot: {}", e.getMessage());
            return Map.of();
        }
    }

    @Transactional(readOnly = true)
    public VehiculoUsadoDTO obtenerPorId(Long id) {
        VehiculoUsado v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usado con id " + id + " no encontrado"));

        if (!Boolean.TRUE.equals(v.getActivo())) {
            throw new ResourceNotFoundException("Usado con id " + id + " no está activo");
        }

        BigDecimal precio = obtenerPrecioSeguro(v.getPilotId());
        boolean disponible = verificarDisponibilidad(v.getPilotId());

        return toDTO(v, precio, disponible);
    }

    // ============== Admin ==============

    @Transactional(readOnly = true)
    public List<VehiculoUsadoDTO> listarTodosAdmin() {
        return repository.findAll().stream()
                .map(v -> toDTO(v, obtenerPrecioSeguro(v.getPilotId()), true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehiculoUsadoDTO obtenerPorIdAdmin(Long id) {
        VehiculoUsado v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usado con id " + id + " no encontrado"));
        return toDTO(v, obtenerPrecioSeguro(v.getPilotId()), true);
    }

    @Transactional
    public VehiculoUsadoDTO crear(VehiculoUsadoRequest req) {
        if (repository.existsByPilotId(req.pilotId())) {
            throw new IllegalArgumentException("Ya existe un usado con pilotId " + req.pilotId());
        }
        VehiculoUsado v = new VehiculoUsado();
        v.setPilotId(req.pilotId());
        aplicarCampos(v, req);
        return toDTO(repository.save(v), obtenerPrecioSeguro(v.getPilotId()), true);
    }

    @Transactional
    public VehiculoUsadoDTO actualizar(Long id, VehiculoUsadoRequest req) {
        VehiculoUsado v = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usado con id " + id + " no encontrado"));
        aplicarCampos(v, req);
        return toDTO(repository.save(v), obtenerPrecioSeguro(v.getPilotId()), true);
    }

    @Transactional
    public VehiculoUsadoDTO cambiarActivoPorPilotId(String pilotId, boolean activo) {
        VehiculoUsado v = repository.findByPilotId(pilotId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe usado con pilotId " + pilotId));
        v.setActivo(activo);
        return toDTO(repository.save(v), obtenerPrecioSeguro(pilotId), true);
    }

    @Transactional
    public void eliminarPorPilotId(String pilotId) {
        VehiculoUsado v = repository.findByPilotId(pilotId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe usado con pilotId " + pilotId));
        repository.delete(v);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Usado con id " + id + " no encontrado");
        }
        repository.deleteById(id);
    }

    // ============== Helpers ==============

    private void agregarCaracteristicas(VehiculoUsado v, List<String> items,
                                        CaracteristicaUsado.CategoriaCaracteristica categoria) {
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            CaracteristicaUsado c = new CaracteristicaUsado();
            c.setVehiculo(v);
            c.setCategoria(categoria);
            c.setNombre(items.get(i));
            c.setOrden(i);
            v.getCaracteristicas().add(c);
        }
    }

    private BigDecimal obtenerPrecioSeguro(String pilotId) {
        try {
            Optional<PilotStockDTO> stock = stockService.buscarPorPilotId(pilotId);
            return stock.map(s -> parsePrecio(s.getSalePrice())).orElse(null);
        } catch (Exception e) {
            log.warn("No se pudo obtener precio para pilotId {}: {}", pilotId, e.getMessage());
            return null;
        }
    }

    private boolean verificarDisponibilidad(String pilotId) {
        try {
            return stockService.buscarPorPilotId(pilotId)
                    .map(PilotStockDTO::isDisponible)
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    private BigDecimal parsePrecio(String precio) {
        try {
            return precio != null ? new BigDecimal(precio) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void aplicarCampos(VehiculoUsado v, VehiculoUsadoRequest req) {
        v.setActivo(Boolean.TRUE.equals(req.activo()));
        v.setMarca(req.marca());
        v.setModelo(req.modelo());
        v.setVersion(req.version());
        v.setAnio(req.anio());
        v.setKm(req.km());
        v.setCombustible(req.combustible());
        v.setColor(req.color());
        v.setTipo(req.tipo());
        v.setCilindrada(req.cilindrada());
        v.setPotencia(req.potencia());
        v.setPuertas(req.puertas());
        v.setDireccion(req.direccion());
        v.setTransmision(req.transmision());
        v.setGarantia(Boolean.TRUE.equals(req.garantia()));
        v.setFinanciacion(Boolean.TRUE.equals(req.financiacion()));
        v.setUnicoDueno(Boolean.TRUE.equals(req.unicoDueno()));
        v.setPatenteMensual(req.patenteMensual());
        v.setPatenteAnual(req.patenteAnual());
        v.setDescripcion(req.descripcion());
        // Solo sobreescribe si el request trae valor — nunca pisa con null
        // (precioRef es un campo interno de filtro de Pilot que el frontend no conoce)
        if (req.precioRef() != null) {
            v.setPrecioRef(req.precioRef());
        }

        v.getImagenes().clear();
        if (req.imagenes() != null) {
            req.imagenes().forEach(img -> {
                ImagenUsado i = new ImagenUsado();
                i.setVehiculo(v);
                i.setUrl(img.url());
                i.setEsPrincipal(Boolean.TRUE.equals(img.esPrincipal()));
                i.setOrden(img.orden() != null ? img.orden() : 0);
                v.getImagenes().add(i);
            });
        }

        // Reemplazamos características
        v.getCaracteristicas().clear();
        if (req.caracteristicas() != null) {
            agregarCaracteristicas(v, req.caracteristicas().seguridad(),
                    CaracteristicaUsado.CategoriaCaracteristica.SEGURIDAD);
            agregarCaracteristicas(v, req.caracteristicas().confort(),
                    CaracteristicaUsado.CategoriaCaracteristica.CONFORT);
            agregarCaracteristicas(v, req.caracteristicas().multimedia(),
                    CaracteristicaUsado.CategoriaCaracteristica.MULTIMEDIA);
        }
    }

    private VehiculoUsadoDTO.VehiculoUsadoCardDTO toCardDTO(VehiculoUsado v, BigDecimal precio) {
        String imagenPrincipal = v.getImagenes() == null ? null :
                v.getImagenes().stream()
                        .filter(i -> Boolean.TRUE.equals(i.getEsPrincipal()))
                        .findFirst()
                        .map(ImagenUsado::getUrl)
                        .orElse(v.getImagenes().stream()
                                .min(Comparator.comparingInt(i -> i.getOrden() == null ? 0 : i.getOrden()))
                                .map(ImagenUsado::getUrl)
                                .orElse(null));

        return new VehiculoUsadoDTO.VehiculoUsadoCardDTO(
                v.getId(), v.getPilotId(), v.getMarca(), v.getModelo(), v.getVersion(),
                v.getAnio(), v.getKm(), precio, v.getTipo(), v.getCombustible(),
                v.getTransmision(), v.getColor(), v.getGarantia(), v.getFinanciacion(),
                v.getUnicoDueno(), imagenPrincipal
        );
    }

    private VehiculoUsadoDTO.CaracteristicasDTO mapearCaracteristicas(VehiculoUsado v) {
        if (v.getCaracteristicas() == null) {
            return new VehiculoUsadoDTO.CaracteristicasDTO(List.of(), List.of(), List.of());
        }
        return new VehiculoUsadoDTO.CaracteristicasDTO(
                filtrarCarac(v, CaracteristicaUsado.CategoriaCaracteristica.SEGURIDAD),
                filtrarCarac(v, CaracteristicaUsado.CategoriaCaracteristica.CONFORT),
                filtrarCarac(v, CaracteristicaUsado.CategoriaCaracteristica.MULTIMEDIA)
        );
    }

    private List<String> filtrarCarac(VehiculoUsado v,
                                      CaracteristicaUsado.CategoriaCaracteristica cat) {
        return v.getCaracteristicas().stream()
                .filter(c -> c.getCategoria() == cat)
                .sorted(Comparator.comparingInt(c -> c.getOrden() == null ? 0 : c.getOrden()))
                .map(CaracteristicaUsado::getNombre)
                .collect(Collectors.toList());
    }



    private VehiculoUsadoDTO toDTO(VehiculoUsado v, BigDecimal precio, boolean disponible) {
        List<VehiculoUsadoDTO.ImagenUsadoDTO> imgs = v.getImagenes() == null ? List.of() :
                v.getImagenes().stream()
                        .sorted(Comparator.comparingInt(i -> i.getOrden() == null ? 0 : i.getOrden()))
                        .map(i -> new VehiculoUsadoDTO.ImagenUsadoDTO(
                                i.getId(), i.getUrl(), i.getEsPrincipal(), i.getOrden()))
                        .collect(Collectors.toList());



        return new VehiculoUsadoDTO(
                v.getId(), v.getPilotId(), v.getActivo(),
                v.getMarca(), v.getModelo(), v.getVersion(),
                v.getAnio(), v.getKm(), v.getCombustible(), v.getColor(),
                precio, disponible,
                v.getTipo(), v.getCilindrada(), v.getPotencia(), v.getPuertas(),
                v.getDireccion(), v.getTransmision(), v.getGarantia(), v.getFinanciacion(),
                v.getUnicoDueno(), v.getPatenteMensual(), v.getPatenteAnual(),
                v.getDescripcion(), imgs,
                mapearCaracteristicas(v)
        );


    }
}
