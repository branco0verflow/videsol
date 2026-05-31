package com.videsol.backend.dto.response;

import java.util.List;

public record PaginaDTO<T>(
        List<T> contenido,
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        int porPagina,
        boolean esUltima
) {}

