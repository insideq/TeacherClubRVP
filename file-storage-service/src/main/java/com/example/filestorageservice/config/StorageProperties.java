package com.example.filestorageservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Настройки доступа к S3-совместимому хранилищу.
 * endpoints — список нод кластера MinIO (или один адрес для AWS S3).
 */
@ConfigurationProperties(prefix = "storage.s3")
public record StorageProperties(
        List<String> endpoints,
        String region,
        String accessKey,
        String secretKey,
        String bucket) {
}
