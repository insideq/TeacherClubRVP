package com.example.reportservice.controller;

import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.messaging.MessagingUnavailableException;
import com.example.reportservice.service.ReportService;
import com.example.reportservice.service.ReportService.ReportSubmission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/teachers/csv")
    @Operation(summary = "Сформировать отчёт и отправить его в хранилище через очередь (асинхронно)")
    public ResponseEntity<?> teachersReportCsv() {
        try {
            ReportSubmission submission = service.publishCsvReport();

            Map<String, Object> response = new HashMap<>();
            response.put("reportId", submission.reportId());
            response.put("status", "PENDING");
            response.put("filename", submission.filename());
            response.put("downloadUrl", submission.downloadUrl());
            response.put("note", "Отчёт отправлен в очередь; файл появится в хранилище асинхронно");

            // 202 Accepted: запрос принят, обработка ещё идёт
            return ResponseEntity.accepted().body(response);
        } catch (MessagingUnavailableException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "QUEUE_UNAVAILABLE", e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
