package com.videsol.backend.service;

import com.videsol.backend.config.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final AwsS3Properties props;

    /**
     * Sube un archivo a S3 y devuelve la URL pública.
     *
     * @param file      archivo recibido del frontend
     * @param carpeta   subcarpeta en el bucket: "okm", "usados"
     * @return URL pública del archivo subido
     */
    public String subirImagen(MultipartFile file, String carpeta) {
        validarArchivo(file);

        String extension = obtenerExtension(file.getOriginalFilename());
        String key = carpeta + "/" + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            String url = "https://" + props.bucket() + ".s3." + props.region()
                    + ".amazonaws.com/" + key;

            log.info("Imagen subida a S3: {}", url);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Error leyendo el archivo: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error subiendo imagen a S3: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de S3 por su URL pública.
     */
    public void eliminarImagen(String url) {
        if (url == null || url.isBlank()) return;
        try {
            String key = extraerKey(url);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .build());
            log.info("Imagen eliminada de S3: {}", key);
        } catch (Exception e) {
            log.error("Error eliminando imagen de S3: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se permiten imágenes");
        }

        // Máximo 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen no puede superar 5MB");
        }
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String extraerKey(String url) {
        // https://bucket.s3.region.amazonaws.com/carpeta/archivo.jpg
        // → carpeta/archivo.jpg
        String base = "amazonaws.com/";
        int idx = url.indexOf(base);
        return idx >= 0 ? url.substring(idx + base.length()) : url;
    }
}