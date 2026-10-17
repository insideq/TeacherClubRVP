package com.example.reportservice.messaging;

import java.time.Instant;

/**
 * Сообщение с данными отчёта, которое уезжает в файловое хранилище через очередь.
 * reportId — он же основа ключа объекта в S3 (reportId + ".csv").
 * Детерминированный ключ делает обработку идемпотентной: повторная доставка
 * того же сообщения просто перезапишет тот же объект, дубля не появится.
 */
public record ReportMessage(
        String reportId,
        String filename,
        String csv,
        Instant generatedAt,
        int teachersCount,
        long grandTotal) {
}
