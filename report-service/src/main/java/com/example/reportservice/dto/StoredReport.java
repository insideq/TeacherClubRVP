package com.example.reportservice.dto;

import java.time.Instant;

/**
 * Сохранённый отчёт, полученный из файлового хранилища.
 */
public record StoredReport(
        String reportId,
        long size,
        Instant lastModified,
        String content) {
}
