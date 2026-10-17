package com.example.filestorageservice.messaging;

import java.time.Instant;

/**
 * Сообщение с данными отчёта, пришедшее из очереди.
 *
 * reportId — он же основа ключа объекта в S3 (reportId + ".csv").
 * Детерминированный ключ делает обработку идемпотентной: повторная доставка
 * того же сообщения перезапишет тот же объект, дубля не появится.
 */
public record ReportMessage(
        String reportId,
        String filename,
        String csv,
        Instant generatedAt,
        int teachersCount,
        long grandTotal) {

    public String objectKey() {
        return reportId + ".csv";
    }
}
