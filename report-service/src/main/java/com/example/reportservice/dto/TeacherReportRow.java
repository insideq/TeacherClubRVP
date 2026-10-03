package com.example.reportservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherReportRow {
    private String fullName;
    private Long totalStudents;
}