package com.example.reportservice.service;

import com.example.reportservice.client.TeacherServiceClient;
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
}