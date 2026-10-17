package com.example.reportservice.service;

import com.example.reportservice.client.AsyncStorageClient;
import com.example.reportservice.client.ReportNotReadyException;
import com.example.reportservice.client.StorageUnavailableException;
import com.example.reportservice.client.TeacherServiceClient;
import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.dto.StoredReport;
import com.example.reportservice.dto.TeacherDto;
import com.example.reportservice.dto.TeacherReportRow;
import com.example.reportservice.messaging.ReportMessage;
import com.example.reportservice.messaging.ReportPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final long REPORT_TIMEOUT_SECONDS = 10;

    private final TeacherServiceClient teacherServiceClient;
    private final ReportPublisher reportPublisher;
    private final AsyncStorageClient asyncStorageClient;

    public ReportResponse buildTeacherReport() {
        List<TeacherDto> teachers = teacherServiceClient.getAllTeachers();

        Map<String, Long> grouped = teachers.stream()
                .collect(Collectors.groupingBy(
                        TeacherDto::getFullName,
                        Collectors.summingLong(t -> t.getStudentsCount() != null ? t.getStudentsCount() : 0)
                ));

        List<TeacherReportRow> rows = grouped.entrySet().stream()
                .map(e -> TeacherReportRow.builder()
                        .fullName(e.getKey())
                        .totalStudents(e.getValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getTotalStudents(), a.getTotalStudents()))
                .toList();

        long grandTotal = rows.stream()
                .mapToLong(TeacherReportRow::getTotalStudents)
                .sum();

        return ReportResponse.builder()
                .title("Отчёт: количество обучающихся у каждого руководителя")
                .generatedAt(LocalDateTime.now())
                .rows(rows)
                .grandTotal(grandTotal)
                .teachersCount(rows.size())
                .build();
    }

    /**
     * Считает отчёт, публикует его в очередь и сразу возвращает идентификатор.
     * Файл появится в хранилище асинхронно; ключ детерминированный — reportId + ".csv",
     * поэтому клиент может просто дождаться появления файла по этому адресу.
     */
    public ReportSubmission publishCsvReport() {
        ReportResponse report = buildTeacherReport();

        String reportId = UUID.randomUUID().toString();
        String filename = "report-" + Instant.now().toEpochMilli() + ".csv";

        ReportMessage message = new ReportMessage(
                reportId,
                filename,
                convertToCsv(report),
                Instant.now(),
                report.getTeachersCount(),
                report.getGrandTotal());

        reportPublisher.publish(message);

        return new ReportSubmission(reportId, filename, "/api/files/" + reportId + ".csv");
    }

    /**
     * Забирает сохранённый отчёт из файлового хранилища.
     * Содержимое и метаданные запрашиваются параллельно (thenCombine) и склеиваются,
     * когда готовы оба результата: суммарное время равно самому долгому запросу,
     * а не их сумме.
     */
    public StoredReport fetchStoredReport(String reportId) {
        try {
            return asyncStorageClient.fetchContent(reportId)
                    .thenCombine(
                            asyncStorageClient.fetchInfo(reportId),
                            (bytes, info) -> new StoredReport(
                                    reportId, bytes.length, info.lastModified(), asText(bytes)))
                    .orTimeout(REPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        } catch (CompletionException e) {
            throw translate(e);
        }
    }

    /**
     * То же, но сначала дожидается появления файла (асинхронный опрос с паузами),
     * и только потом запрашивает метаданные — это уже цепочка thenCompose.
     * Удобно сразу после POST из лабы 5, который возвращает 202.
     */
    public StoredReport awaitStoredReport(String reportId, Duration timeout) {
        try {
            return asyncStorageClient.awaitContent(reportId, timeout)
                    .thenCompose(bytes -> asyncStorageClient.fetchInfo(reportId)
                            .thenApply(info -> new StoredReport(
                                    reportId, bytes.length, info.lastModified(), asText(bytes))))
                    .join();
        } catch (CompletionException e) {
            throw translate(e);
        }
    }

    private String asText(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Превращает причину из CompletableFuture в понятное доменное исключение. */
    private RuntimeException translate(CompletionException e) {
        Throwable cause = AsyncStorageClient.unwrap(e);
        if (cause instanceof ReportNotReadyException || cause instanceof StorageUnavailableException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof TimeoutException) {
            return new StorageUnavailableException(
                    "Файловое хранилище не ответило за " + REPORT_TIMEOUT_SECONDS + " c", cause);
        }
        return new StorageUnavailableException(
                "Ошибка обращения к файловому хранилищу: " + cause.getMessage(), cause);
    }

    private String convertToCsv(ReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("fullName,totalStudents\n");
        for (TeacherReportRow row : report.getRows()) {
            sb.append(escapeCsv(row.getFullName()))
                    .append(",")
                    .append(row.getTotalStudents())
                    .append("\n");
        }
        sb.append("TOTAL,").append(report.getGrandTotal()).append("\n");
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public record ReportSubmission(String reportId, String filename, String downloadUrl) {
    }
}
