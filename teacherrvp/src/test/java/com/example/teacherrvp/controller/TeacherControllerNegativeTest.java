package com.example.teacherrvp.controller;

import com.example.teacherrvp.dto.TeacherDto;
import com.example.teacherrvp.service.TeacherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherController.class)
@DisplayName("TeacherController — негативные тесты")
class TeacherControllerNegativeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeacherService service;

    @Test
    @DisplayName("GET /api/teachers/{id} — 404, если сервис бросил NoSuchElementException")
    void getById_notFound_returns404() throws Exception {
        when(service.getById(999L))
                .thenThrow(new NoSuchElementException("Руководитель не найден: id=999"));

        mockMvc.perform(get("/api/teachers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Руководитель не найден: id=999"));
    }

    @Test
    @DisplayName("POST /api/teachers — 400, если fullName пустой")
    void create_emptyFullName_returns400() throws Exception {
        TeacherDto bad = TeacherDto.builder()
                .fullName("")
                .clubName("Кружок")
                .studentsCount(10)
                .build();

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/teachers — 400, если clubName null")
    void create_nullClubName_returns400() throws Exception {
        TeacherDto bad = TeacherDto.builder()
                .fullName("Тест")
                .clubName(null)
                .studentsCount(10)
                .build();

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/teachers — 400, если studentsCount отрицательный")
    void create_negativeStudentsCount_returns400() throws Exception {
        TeacherDto bad = TeacherDto.builder()
                .fullName("Тест")
                .clubName("Кружок")
                .studentsCount(-5)
                .build();

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/teachers — 400, если studentsCount null")
    void create_nullStudentsCount_returns400() throws Exception {
        TeacherDto bad = TeacherDto.builder()
                .fullName("Тест")
                .clubName("Кружок")
                .studentsCount(null)
                .build();

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/teachers — 400, если fullName длиннее 200 символов")
    void create_tooLongFullName_returns400() throws Exception {
        String tooLong = "A".repeat(201);

        TeacherDto bad = TeacherDto.builder()
                .fullName(tooLong)
                .clubName("Кружок")
                .studentsCount(10)
                .build();

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/teachers/{id} — 404, если записи нет")
    void update_notFound_returns404() throws Exception {
        TeacherDto req = TeacherDto.builder()
                .fullName("Кто-то")
                .clubName("Что-то")
                .studentsCount(1)
                .build();

        when(service.update(eq(999L), any(TeacherDto.class)))
                .thenThrow(new NoSuchElementException("Руководитель не найден: id=999"));

        mockMvc.perform(put("/api/teachers/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/teachers/{id} — 404, если записи нет")
    void delete_notFound_returns404() throws Exception {
        doThrow(new NoSuchElementException("Руководитель не найден: id=999"))
                .when(service).delete(999L);

        mockMvc.perform(delete("/api/teachers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/teachers — 400, если тело запроса невалидный JSON")
    void create_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }
}