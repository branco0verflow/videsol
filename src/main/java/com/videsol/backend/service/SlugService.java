package com.videsol.backend.service;

import com.videsol.backend.repository.VehiculoOkmRepository;
import com.videsol.backend.repository.VehiculoUsadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SlugService {

    private final VehiculoOkmRepository okmRepository;
    private final VehiculoUsadoRepository usadoRepository;

    /**
     * Genera un slug único para 0KM.
     * Ej: "RENAULT NUEVO KWID BITONO 2026" → "renault-nuevo-kwid-bitono-2026"
     */
    public String generarSlugOkm(String marca, String modelo,
                                 String version, Integer anio) {
        String base = slugify(joinear(marca, modelo, version,
                anio != null ? String.valueOf(anio) : null));
        return unicoOkm(base, null);
    }

    /**
     * Genera un slug único para usados.
     */
    public String generarSlugUsado(String marca, String modelo,
                                   String version, Integer anio) {
        String base = slugify(joinear(marca, modelo, version,
                anio != null ? String.valueOf(anio) : null));
        return unicoUsado(base, null);
    }

    // =========================================================================

    private String unicoOkm(String base, Long excludeId) {
        String candidate = base;
        int i = 2;
        while (true) {
            String c = candidate;
            boolean existe = okmRepository.findAll().stream()
                    .filter(v -> excludeId == null || !v.getId().equals(excludeId))
                    .anyMatch(v -> c.equals(v.getSlug()));
            if (!existe) return candidate;
            candidate = base + "-" + i++;
        }
    }

    private String unicoUsado(String base, Long excludeId) {
        String candidate = base;
        int i = 2;
        while (true) {
            String c = candidate;
            boolean existe = usadoRepository.findAll().stream()
                    .filter(v -> excludeId == null || !v.getId().equals(excludeId))
                    .anyMatch(v -> c.equals(v.getSlug()));
            if (!existe) return candidate;
            candidate = base + "-" + i++;
        }
    }

    private String joinear(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) return "vehiculo";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String sinAcentos = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized).replaceAll("");
        return sinAcentos.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}