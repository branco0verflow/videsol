package com.videsol.backend.controller;

import com.videsol.backend.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/imagenes")
@RequiredArgsConstructor
@Tag(name = "Admin - Imágenes", description = "Subida de imágenes a S3")
public class ImagenController {

    private final S3Service s3Service;

    /**
     * Sube múltiples imágenes y devuelve todas las URLs.
     * POST /api/admin/imagenes/upload?carpeta=okm
     * Body: form-data, campo "files" con varias imágenes
     */
    @PostMapping("/upload")
    @Operation(summary = "Subir una o varias imágenes a S3")
    public ResponseEntity<Map<String, List<String>>> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "okm") String carpeta) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No se enviaron archivos");
        }

        if (files.size() > 10) {
            throw new IllegalArgumentException("Máximo 10 imágenes por vez");
        }

        List<String> urls = files.stream()
                .map(file -> s3Service.subirImagen(file, carpeta))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("urls", urls));
    }

    @DeleteMapping
    @Operation(summary = "Eliminar imagen de S3")
    public ResponseEntity<Void> delete(@RequestParam String url) {
        s3Service.eliminarImagen(url);
        return ResponseEntity.noContent().build();
    }
}