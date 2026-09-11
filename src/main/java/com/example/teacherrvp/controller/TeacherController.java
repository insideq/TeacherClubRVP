package com.example.teacherrvp.controller;

import com.example.teacherrvp.dto.TeacherRequest;
import com.example.teacherrvp.dto.TeacherResponse;
import com.example.teacherrvp.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Руководители кружков", description = "CRUD-операции по руководителям кружков")
public class TeacherController {

    private final TeacherService service;

    @GetMapping
    @Operation(summary = "Список всех руководителей кружков")
    public List<TeacherResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить руководителя по id")
    public TeacherResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @Operation(summary = "Принять нового руководителя кружка")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные (перевод в другой кружок / кол-во обучающихся)")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить руководителя")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}