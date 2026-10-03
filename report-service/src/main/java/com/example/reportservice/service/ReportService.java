package com.example.reportservice.service;

import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.dto.TeacherReportRow;
import com.example.reportservice.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TeacherRepository repository;

    public ReportResponse buildTeacherReport() {
        List<Object[]> raw = repository.sumStudentsByTeacher();

        List<TeacherReportRow> rows = raw.stream()
                .map(r -> TeacherReportRow.builder()
                        .fullName((String) r[0])
                        .totalStudents(((Number) r[1]).longValue())
                        .build())
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