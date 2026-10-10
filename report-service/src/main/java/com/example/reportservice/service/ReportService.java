package com.example.reportservice.service;

import com.example.reportservice.client.TeacherServiceClient;
import com.example.reportservice.client.FileStorageClient;
import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.dto.TeacherDto;
import com.example.reportservice.dto.TeacherReportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TeacherServiceClient teacherServiceClient;
    private final FileStorageClient fileStorageClient;

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

    public String buildAndSaveCsvReport() {
        ReportResponse report = buildTeacherReport();
        String csv = convertToCsv(report);

        // Отправка в file-storage-service
        String filename = "report-" + System.currentTimeMillis() + ".csv";

        return fileStorageClient.uploadCsv(filename, csv);
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
}