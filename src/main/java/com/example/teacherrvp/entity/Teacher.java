package com.example.teacherrvp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "club_name", nullable = false, length = 200)
    private String clubName;

    @Column(name = "students_count", nullable = false)
    private Integer studentsCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}