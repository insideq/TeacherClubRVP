package com.example.teacherrvp.repository;

import com.example.teacherrvp.entity.Teacher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("TeacherRepository — тесты на H2")
class TeacherRepositoryTest {

    @Autowired
    private TeacherRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("save — сохраняет и присваивает id")
    void save_assignsId() {
        Teacher t = Teacher.builder()
                .fullName("Иванов Иван Иванович")
                .clubName("Робототехника")
                .studentsCount(15)
                .qualification("Высшая")
                .build();

        Teacher saved = repository.save(t);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);
        assertThat(saved.getFullName()).isEqualTo("Иванов Иван Иванович");
    }

    @Test
    @DisplayName("findById — находит по id")
    void findById_found() {
        Teacher t = Teacher.builder()
                .fullName("Петрова Анна Сергеевна")
                .clubName("Физика")
                .studentsCount(10)
                .build();
        Long id = repository.save(t).getId();
        entityManager.flush();
        entityManager.clear();

        Optional<Teacher> found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Петрова Анна Сергеевна");
    }

    @Test
    @DisplayName("findById — пусто, если записи нет")
    void findById_notFound() {
        Optional<Teacher> found = repository.findById(99999L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll — возвращает все записи")
    void findAll_returnsAll() {
        repository.save(Teacher.builder().fullName("A").clubName("X").studentsCount(1).build());
        repository.save(Teacher.builder().fullName("B").clubName("Y").studentsCount(2).build());
        repository.save(Teacher.builder().fullName("C").clubName("Z").studentsCount(3).build());

        List<Teacher> all = repository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("existsById — true/false правильно")
    void existsById_worksCorrectly() {
        Teacher t = repository.save(Teacher.builder()
                .fullName("Сидоров")
                .clubName("Математика")
                .studentsCount(5)
                .build());

        assertThat(repository.existsById(t.getId())).isTrue();
        assertThat(repository.existsById(99999L)).isFalse();
    }

    @Test
    @DisplayName("deleteById — удаляет запись")
    void deleteById_removes() {
        Teacher t = repository.save(Teacher.builder()
                .fullName("Удаляем")
                .clubName("Кружок")
                .studentsCount(1)
                .build());
        Long id = t.getId();

        repository.deleteById(id);
        entityManager.flush();

        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    @DisplayName("count — возвращает правильное число")
    void count_returnsCorrectNumber() {
        assertThat(repository.count()).isEqualTo(0);

        repository.save(Teacher.builder().fullName("A").clubName("X").studentsCount(1).build());
        repository.save(Teacher.builder().fullName("B").clubName("Y").studentsCount(2).build());

        assertThat(repository.count()).isEqualTo(2);
    }
}