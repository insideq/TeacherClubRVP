package com.example.reportservice.client;

import java.time.Instant;

/**
 * Метаданные объекта из файлового хранилища (ответ GET /api/files).
 */
public record StoredFileInfo(String key, long size, Instant lastModified) {
}
