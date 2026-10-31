package com.example.teacherrvp.service;

import com.example.mergesort.ParallelMergeSort;
import com.example.mergesort.SortResult;
import com.example.teacherrvp.dto.BenchmarkResponse;
import com.example.teacherrvp.dto.SortedTeachersResponse;
import com.example.teacherrvp.dto.TeacherDto;
import com.example.teacherrvp.entity.Teacher;
import com.example.teacherrvp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService {

    private static final int MAX_THREADS = 64;
    private static final int[] BENCHMARK_THREADS = {1, 2, 4, 8};
    private static final int MAX_BENCHMARK_SIZE = 500_000;

    private final TeacherRepository repository;

    public List<TeacherDto> getAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public TeacherDto getById(Long id) {
        Teacher t = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Руководитель не найден: id=" + id));
        return toDto(t);
    }

    public TeacherDto create(TeacherDto req) {
        Teacher t = Teacher.builder()
                .fullName(req.getFullName())
                .clubName(req.getClubName())
                .studentsCount(req.getStudentsCount())
                .qualification(req.getQualification())
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(repository.save(t));
    }

    public TeacherDto update(Long id, TeacherDto req) {
        Teacher t = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Руководитель не найден: id=" + id));
        t.setFullName(req.getFullName());
        t.setClubName(req.getClubName());
        t.setStudentsCount(req.getStudentsCount());
        t.setQualification(req.getQualification());
        return toDto(repository.save(t));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Руководитель не найден: id=" + id);
        }
        repository.deleteById(id);
    }

    /**
     * Данные пользователей, отсортированные нашей библиотекой параллельной сортировки.
     * Число потоков задаёт клиент; в ответ кладём время работы и раунды слияния.
     */
    public SortedTeachersResponse getSorted(String sortBy, String order, int threads) {
        List<TeacherDto> teachers = getAll();
        Comparator<TeacherDto> comparator = comparator(sortBy, order);

        SortResult<TeacherDto> result = ParallelMergeSort.sort(teachers, comparator, clampThreads(threads));

        log.info("Параллельная сортировка: {} элементов, {} потоков, {} мс, раунды {}",
                result.size(), result.threads(), result.durationMs(), result.rounds());

        return new SortedTeachersResponse(
                result.items(),
                result.size(),
                result.threads(),
                result.durationMs(),
                result.durationNanos(),
                result.rounds());
    }

    /**
     * Демонстрация ровно на примере из задания: любой список id через запятую.
     * Возвращает раунды слияния, чтобы было видно «5 потоков → 3 → 2 → 1».
     */
    public Map<String, Object> sortDemo(List<Integer> ids, int threads) {
        SortResult<Integer> result = ParallelMergeSort.sort(ids, Comparator.naturalOrder(), clampThreads(threads));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", ids);
        body.put("sorted", result.items());
        body.put("threads", result.threads());
        body.put("rounds", result.rounds());
        body.put("durationMs", result.durationMs());
        body.put("durationNanos", result.durationNanos());
        return body;
    }

    /**
     * Замер на синтетическом массиве.
     */
    public BenchmarkResponse benchmark(int size) {
        int elements = Math.clamp(size, 1_000, MAX_BENCHMARK_SIZE);
        List<Long> data = new Random(42).longs(elements, 0, elements * 10L).boxed().toList();

        List<BenchmarkResponse.Row> rows = new ArrayList<>();
        for (int threads : BENCHMARK_THREADS) {
            SortResult<Long> result = ParallelMergeSort.sort(data, Comparator.naturalOrder(), threads);
            rows.add(new BenchmarkResponse.Row(
                    threads,
                    result.durationMs(),
                    result.durationNanos(),
                    result.rounds().size()));
        }

        int processors = Runtime.getRuntime().availableProcessors();
        log.info("Бенчмарк сортировки: {} элементов, доступно ядер {}, потоки {}",
                elements, processors, BENCHMARK_THREADS);

        return new BenchmarkResponse(elements, processors, rows);
    }

    private Comparator<TeacherDto> comparator(String sortBy, String order) {
        Comparator<TeacherDto> base = switch (sortBy == null ? "id" : sortBy.toLowerCase()) {
            case "fullname" -> Comparator.comparing(
                    TeacherDto::getFullName, Comparator.nullsLast(String::compareTo));
            case "clubname" -> Comparator.comparing(
                    TeacherDto::getClubName, Comparator.nullsLast(String::compareTo));
            case "studentscount" -> Comparator.comparing(
                    TeacherDto::getStudentsCount, Comparator.nullsLast(Integer::compareTo));
            default -> Comparator.comparing(
                    TeacherDto::getId, Comparator.nullsLast(Long::compareTo));
        };
        return "desc".equalsIgnoreCase(order) ? base.reversed() : base;
    }

    private int clampThreads(int threads) {
        return Math.clamp(threads, 1, MAX_THREADS);
    }

    private TeacherDto toDto(Teacher t) {
        return TeacherDto.builder()
                .id(t.getId())
                .fullName(t.getFullName())
                .clubName(t.getClubName())
                .studentsCount(t.getStudentsCount())
                .qualification(t.getQualification())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
