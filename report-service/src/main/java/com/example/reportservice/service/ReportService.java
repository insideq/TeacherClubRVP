package com.example.reportservice.service;

import com.example.reportservice.client.TeacherServiceClient;
import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.dto.TeacherDto;
import com.example.reportservice.dto.TeacherReportRow;
import com.example.reportservice.messaging.ReportMessage;
import com.example.reportservice.messaging.ReportPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TeacherServiceClient teacherServiceClient;
    private final ReportPublisher reportPublisher;

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
