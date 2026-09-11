package com.example.teacherrvp.repository;

import com.example.teacherrvp.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}