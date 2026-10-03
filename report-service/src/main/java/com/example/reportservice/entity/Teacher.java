package com.example.reportservice.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "qualification", length = 200)
    private String qualification;
}