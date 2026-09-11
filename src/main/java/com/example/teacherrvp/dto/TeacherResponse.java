package com.example.teacherrvp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherResponse {
    private Long id;
    private String fullName;
    private String clubName;
    private Integer studentsCount;
    private LocalDateTime createdAt;
}