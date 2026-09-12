package com.example.teacherrvp.controller;

import com.example.teacherrvp.dto.TeacherDto;
import com.example.teacherrvp.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Руководители кружков", description = "CRUD-операции по руководителям кружков")
public class TeacherController {

    private final TeacherService service;

    @GetMapping
    @Operation(summary = "Список всех руководителей кружков")
    public ResponseEntity<?> getAll() {
        try {
            List<TeacherDto> result = service.getAll();
            return ResponseEntity.ok(result);
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