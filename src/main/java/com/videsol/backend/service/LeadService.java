package com.videsol.backend.service;

import com.videsol.backend.dto.request.LeadRequest;
import com.videsol.backend.dto.response.LeadResponseDTO;
import com.videsol.backend.dto.response.PrecioDTO;
import com.videsol.backend.entity.VehiculoOkm;
import com.videsol.backend.entity.VehiculoUsado;
import com.videsol.backend.exception.ResourceNotFoundException;
import com.videsol.backend.repository.VehiculoOkmRepository;
import com.videsol.backend.repository.VehiculoUsadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Crea leads en el CRM Pilot.
 *
 * Flujo:
 * 1. Recibe los datos del usuario + vehiculoId/tipo
 * 2. Busca el vehículo en la DB para obtener marca/modelo/code
 * 3. Complementa con precio fresco de Pilot si es 0KM
 * 4. Envía el lead a Pilot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final WebClient pilotWebClient;
    private final PilotAuthService authService;
    private final VehiculoOkmRepository okmRepository;
    private final VehiculoUsadoRepository usadoRepository;
    private final PilotPriceListService precioService;

    @Value("${pilot.lead.suborigin-code-okm:}")
    private String suboriginCodeOkm;

    @Value("${pilot.lead.suborigin-code-usado:}")
    private String suboriginCodeUsado;

    @Value("${pilot.lead.business-type-nuevo:nuevo}")
    private String businessTypeNuevo;

    @Value("${pilot.lead.business-type-usado:usado}")
    private String businessTypeUsado;

    public LeadResponseDTO crearLead(LeadRequest req) {
        try {
            // 1. Obtener datos del vehículo según tipo
            VehiculoInfo info = obtenerInfoVehiculo(req);

            // 2. Armar el body para Pilot
            Map<String, Object> data = armarData(req, info);

            // 3. Armar request completo con header
            Map<String, Object> request = Map.of(
                    "data", data,
                    "header", Map.of(
                            "FlowName", "lead_create",
                            "SequenceId", List.of(),
                            "TimeStamp", List.of(),
                            "TrackingId", UUID.randomUUID().toString().toUpperCase(),
                            "access_token", authService.getToken()
                    )
            );

            // 4. Enviar a Pilot
            String rawJson = pilotWebClient.post()
                    .uri("/v1/welcomes/create.php")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Lead response from Pilot: {}", rawJson);

            // 5. Parsear respuesta
            return parsearRespuesta(rawJson);

        } catch (ResourceNotFoundException e) {
            log.warn("Vehículo no encontrado al crear lead: {}", e.getMessage());
            return LeadResponseDTO.error("El vehículo consultado no está disponible.");
        } catch (Exception e) {
            log.error("Error creando lead en Pilot", e);
            return LeadResponseDTO.error("Hubo un error al registrar tu consulta. Intentá de nuevo.");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private VehiculoInfo obtenerInfoVehiculo(LeadRequest req) {
        if ("okm".equalsIgnoreCase(req.vehiculoTipo())) {
            VehiculoOkm v = okmRepository.findById(req.vehiculoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Vehículo 0KM con id " + req.vehiculoId() + " no encontrado"));

            // Intentamos obtener precio y marca/modelo frescos de Pilot
            String marca = null, modelo = null, version = null, code = null, precio = null;
            try {
                List<PrecioDTO> precios = precioService.listarPreciosPorCodigo(v.getCode());
                if (!precios.isEmpty()) {
                    PrecioDTO p = precios.get(0);
                    marca   = p.marca();
                    modelo  = p.modelo();
                    version = p.name();
                    code    = v.getCode();
                    precio  = p.precio();
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener precio para lead OKM {}", v.getCode());
            }

            // Fallback a marcaRef si Pilot no respondió
            if (marca == null) marca = v.getMarcaRef();

            return new VehiculoInfo(marca, modelo, version, code, precio, businessTypeNuevo);

        } else if ("usado".equalsIgnoreCase(req.vehiculoTipo())) {
            VehiculoUsado v = usadoRepository.findById(req.vehiculoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Vehículo usado con id " + req.vehiculoId() + " no encontrado"));

            return new VehiculoInfo(
                    v.getMarca(), v.getModelo(), v.getVersion(),
                    null,  // los usados no tienen code de lista de precios
                    null,
                    businessTypeUsado
            );
        } else {
            throw new IllegalArgumentException("vehiculoTipo debe ser 'okm' o 'usado'");
        }
    }

    private Map<String, Object> armarData(LeadRequest req, VehiculoInfo info) {
        Map<String, Object> data = new LinkedHashMap<>();

        // Datos del cliente
        data.put("welcome_firstname", req.nombre());
        data.put("welcome_lastname",  req.apellido());
        data.put("welcome_email",     req.email());
        data.put("welcome_cellphone", req.telefono() != null ? req.telefono() : "");
        data.put("welcome_phone",     req.telefono() != null ? req.telefono() : "");

        // Tipo de contacto: "1" = Electrónico (web)
        data.put("welcome_contact_type_code", "1");

        // Tipo de negocio
        data.put("welcome_business_type_code", info.businessType());

        // Datos del vehículo de interés
        data.put("welcome_car_brand",  info.marca()   != null ? info.marca()   : "");
        data.put("welcome_car_modelo", info.modelo()  != null ? info.modelo()  : "");

        // product_code solo para 0KM
        data.put("welcome_product_code", info.code() != null ? info.code() : "");

        // Descripción del vehículo de interés
        String productoInteres = construirDescripcionVehiculo(info);
        data.put("welcome_product_of_interest", productoInteres);

        // Notas: comentario del usuario + info del vehículo
        String notas = construirNotas(req, info);
        data.put("welcome_notes", notas);

        // Suborigin (código de la web de Videsol — completar cuando Pilot lo provea)
        String suborigin = "usado".equalsIgnoreCase(info.businessType())
                ? suboriginCodeUsado
                : suboriginCodeOkm;
        data.put("welcome_suborigin_code", suborigin != null ? suborigin : "");

        // Consentimientos
        data.put("welcome_notifications_opt_in_consent_flag", req.aceptaNotificaciones() ? 1 : 0);
        data.put("welcome_publicity_opt_in_consent_flag",     req.aceptaPublicidad()     ? 1 : 0);

        // Campos opcionales vacíos requeridos por Pilot
        data.put("welcome_assigned_user",           "");
        data.put("welcome_vendor_name",             "");
        data.put("welcome_vendor_email",            "");
        data.put("welcome_vendor_phone",            "");
        data.put("welcome_provider_service",        "");
        data.put("welcome_provider_url",            "");
        data.put("welcome_client_identity_document","");
        data.put("welcome_tracking_id", "web-videsol-" + req.vehiculoId() + "-" + System.currentTimeMillis());
        data.put("welcome_client_ip",               "");
        data.put("welcome_best_contact_time",       "");
        data.put("bad_flag",                        0);

        return data;
    }

    private String construirDescripcionVehiculo(VehiculoInfo info) {
        StringBuilder sb = new StringBuilder();
        if (info.marca()   != null) sb.append(info.marca()).append(" ");
        if (info.modelo()  != null) sb.append(info.modelo()).append(" ");
        if (info.version() != null) sb.append(info.version()).append(" ");
        if (info.precio()  != null) sb.append("- $").append(info.precio());
        return sb.toString().trim();
    }

    private String construirNotas(LeadRequest req, VehiculoInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("Consulta desde web Videsol. ");
        sb.append("Vehículo: ").append(construirDescripcionVehiculo(info)).append(". ");
        if (req.comentario() != null && !req.comentario().isBlank()) {
            sb.append("Comentario del cliente: ").append(req.comentario());
        }
        return sb.toString();
    }

    private LeadResponseDTO parsearRespuesta(String rawJson) {
        try {
            // Parseamos manualmente para no crear un DTO complejo de respuesta
            if (rawJson == null) return LeadResponseDTO.error("Sin respuesta de Pilot");

            if (rawJson.contains("\"status\":\"success\"")) {
                // Extraemos el ID del lead
                String leadId = null;
                if (rawJson.contains("\"id\":\"")) {
                    int start = rawJson.indexOf("\"id\":\"") + 6;
                    int end   = rawJson.indexOf("\"", start);
                    if (start > 5 && end > start) {
                        leadId = rawJson.substring(start, end);
                    }
                }
                log.info("Lead creado exitosamente en Pilot. ID: {}", leadId);
                return LeadResponseDTO.exito(leadId);
            } else {
                // Intentamos extraer el mensaje de error
                String mensaje = "Error al registrar la consulta en Pilot";
                if (rawJson.contains("\"message\":\"")) {
                    int start = rawJson.indexOf("\"message\":\"") + 11;
                    int end   = rawJson.indexOf("\"", start);
                    if (start > 10 && end > start) {
                        mensaje = rawJson.substring(start, end);
                    }
                }
                log.error("Error de Pilot al crear lead: {}", mensaje);
                return LeadResponseDTO.error("Hubo un error al registrar tu consulta. Intentá de nuevo.");
            }
        } catch (Exception e) {
            log.error("Error parseando respuesta de Pilot al crear lead", e);
            return LeadResponseDTO.error("Hubo un error al registrar tu consulta. Intentá de nuevo.");
        }
    }

    /**
     * Datos del vehículo extraídos de la DB para armar el lead.
     */
    private record VehiculoInfo(
            String marca,
            String modelo,
            String version,
            String code,         // solo para 0KM
            String precio,       // solo para 0KM
            String businessType  // "nuevo" o "usado"
    ) {}
}
