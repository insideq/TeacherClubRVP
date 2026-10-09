package com.example.reportservice.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO ответа teacher-service. Используется при десериализации JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDto {
    private Long id;
    private String fullName;
    private String clubName;
    private Integer studentsCount;
    private String qualification;
    private LocalDateTime createdAt;
}