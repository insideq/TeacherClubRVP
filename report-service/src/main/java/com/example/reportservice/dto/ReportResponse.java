package com.example.reportservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private String title;
    private LocalDateTime generatedAt;
    private List<TeacherReportRow> rows;
    private Long grandTotal;
    private Integer teachersCount;
}