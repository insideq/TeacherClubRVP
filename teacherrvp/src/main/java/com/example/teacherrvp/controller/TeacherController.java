package com.example.teacherrvp.controller;

import com.example.teacherrvp.dto.TeacherDto;
import com.example.teacherrvp.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Руководители кружков", description = "CRUD-операции по руководителям кружков")
public class TeacherController {

    private final TeacherService service;

    @Value("${HOSTNAME:unknown}")
    private String instanceId;


    @GetMapping
    @Operation(summary = "Список всех руководителей кружков")
    public ResponseEntity<?> getAll() {
        log.info("Instance [{}] handled GET /api/teachers", instanceId);
        try {
            List<TeacherDto> result = service.getAll();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    @GetMapping("/sorted")
    @Operation(summary = "Данные пользователей, отсортированные параллельной сортировкой слиянием")
    public ResponseEntity<?> getSorted(
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "threads", defaultValue = "4") int threads) {
        log.info("Instance [{}] handled GET /api/teachers/sorted sortBy={} order={} threads={}",
                instanceId, sortBy, order, threads);
        try {
            return ResponseEntity.ok(service.getSorted(sortBy, order, threads));
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    @GetMapping("/sorted/benchmark")
    @Operation(summary = "Замер сортировки при разном числе потоков (1, 2, 4, 8)")
    public ResponseEntity<?> benchmark(
            @RequestParam(name = "size", defaultValue = "100000") int size) {
        try {
            return ResponseEntity.ok(service.benchmark(size));
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    @GetMapping("/sorted/demo")
    @Operation(summary = "Демонстрация на примере из задания: ids=3,2,6,9,2,3,5,8,2,1,11&threads=5")
    public ResponseEntity<?> sortDemo(
            @RequestParam(name = "ids") String ids,
            @RequestParam(name = "threads", defaultValue = "5") int threads) {
        try {
            List<Integer> parsed = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Integer::valueOf)
                    .toList();
            return ResponseEntity.ok(service.sortDemo(parsed, threads));
        } catch (NumberFormatException e) {
            return error(HttpStatus.BAD_REQUEST, "BAD_IDS",
                    "Параметр ids должен быть списком чисел через запятую, например 3,2,6,9");
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить руководителя по id")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "Принять нового руководителя кружка")
    public ResponseEntity<?> create(@Valid @RequestBody TeacherDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные (перевод в другой кружок / кол-во обучающихся)")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TeacherDto request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить руководителя")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }
}
