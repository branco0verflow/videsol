package com.videsol.backend.dto.pilot;

/**
 * Estructura genérica de request para endpoints de Pilot:
 * {
 *   "data": { ... },
 *   "header": { ... }
 * }
 */
public record PilotRequest<T>(T data, PilotRequestHeader header) {}
