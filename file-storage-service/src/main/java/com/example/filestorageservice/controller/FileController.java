package com.example.filestorageservice.controller;

import com.example.filestorageservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Файловое хранилище", description = "Сохранение и выдача файлов в S3 (MinIO)")
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "Список объектов в хранилище")
    public ResponseEntity<?> list() {
        try {
            return ResponseEntity.ok(fileStorageService.list());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить файл в хранилище")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return error(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Файл пуст");
            }
            String storedName = fileStorageService.store(file);

            Map<String, Object> response = new HashMap<>();
            response.put("filename", storedName);
            response.put("originalFilename", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("downloadUrl", "/api/files/" + storedName);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            log.error("Upload failed", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", e.getMessage());
        }
    }

    @GetMapping("/{filename}")
    @Operation(summary = "Скачать файл из хранилища")
    public ResponseEntity<?> download(@PathVariable String filename) {
        try {
            byte[] content = fileStorageService.load(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (IOException e) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
        }
    }

    @DeleteMapping("/{filename}")
    @Operation(summary = "Удалить файл из хранилища")
    public ResponseEntity<?> delete(@PathVariable String filename) {
        try {
            fileStorageService.delete(filename);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
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
