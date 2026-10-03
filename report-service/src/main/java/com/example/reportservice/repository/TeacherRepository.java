package com.example.reportservice.repository;

import com.example.reportservice.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * Отчёт: сколько человек обучается у каждого руководителя.
     * Группировка по ФИО, сумма по students_count, сортировка по убыванию.
     */
    @Query("""
        SELECT t.fullName, SUM(t.studentsCount)
        FROM Teacher t
        GROUP BY t.fullName
        ORDER BY SUM(t.studentsCount) DESC
    """)
    List<Object[]> sumStudentsByTeacher();
}