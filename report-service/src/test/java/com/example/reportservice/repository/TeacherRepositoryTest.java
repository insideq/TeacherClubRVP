package com.example.reportservice.repository;

import com.example.reportservice.entity.Teacher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("TeacherRepository — тесты на H2")
class TeacherRepositoryTest {

    @Autowired
    private TeacherRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("sumStudentsByTeacher — группирует по ФИО и суммирует")
    void sumStudentsByTeacher_aggregates() {
        // Иванов: 15 + 8 + 5 = 28
        repository.save(Teacher.builder().fullName("Иванов Иван Иванович").clubName("Робототехника").studentsCount(15).build());
        repository.save(Teacher.builder().fullName("Иванов Иван Иванович").clubName("Программирование").studentsCount(8).build());
        repository.save(Teacher.builder().fullName("Иванов Иван Иванович").clubName("Математика").studentsCount(5).build());

        // Петрова: 10 + 12 = 22
        repository.save(Teacher.builder().fullName("Петрова Анна Сергеевна").clubName("Физика").studentsCount(10).build());
        repository.save(Teacher.builder().fullName("Петрова Анна Сергеевна").clubName("Химия").studentsCount(12).build());

        entityManager.flush();
        entityManager.clear();

        List<Object[]> result = repository.sumStudentsByTeacher();

        assertThat(result).hasSize(2);
        // сортировка по убыванию суммы: Иванов (28) первым
        assertThat(result.get(0)[0]).isEqualTo("Иванов Иван Иванович");
        assertThat(((Number) result.get(0)[1]).longValue()).isEqualTo(28L);
        assertThat(result.get(1)[0]).isEqualTo("Петрова Анна Сергеевна");
        assertThat(((Number) result.get(1)[1]).longValue()).isEqualTo(22L);
    }

    @Test
    @DisplayName("sumStudentsByTeacher — пустая БД → пустой список")
    void sumStudentsByTeacher_empty() {
        List<Object[]> result = repository.sumStudentsByTeacher();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("sumStudentsByTeacher — один руководитель")
    void sumStudentsByTeacher_singleRow() {
        repository.save(Teacher.builder().fullName("Одиночкин").clubName("Шахматы").studentsCount(7).build());
        entityManager.flush();
        entityManager.clear();

        List<Object[]> result = repository.sumStudentsByTeacher();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()[0]).isEqualTo("Одиночкин");
        assertThat(((Number) result.getFirst()[1]).longValue()).isEqualTo(7L);
    }
}