package com.example.reportservice.controller;

import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Отчёты", description = "Формирование отчётов по руководителям кружков")
public class ReportController {

    private final ReportService service;

    @GetMapping("/teachers")
    @Operation(summary = "Отчёт: сколько человек обучается у каждого руководителя")
    public ResponseEntity<?> teachersReport() {
        try {
            ReportResponse report = service.buildTeacherReport();
            return ResponseEntity.ok(report);
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