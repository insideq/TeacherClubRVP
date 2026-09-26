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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherController.class)
@DisplayName("TeacherController — позитивные тесты")
class TeacherControllerPositiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeacherService service;

    @Test
    @DisplayName("GET /api/teachers — возвращает список и 200 OK")
    void getAll_returnsList() throws Exception {
        List<TeacherDto> mockResult = List.of(
                TeacherDto.builder().id(1L).fullName("Иванов Иван Иванович")
                        .clubName("Робототехника").studentsCount(15).build(),
                TeacherDto.builder().id(2L).fullName("Петрова Анна Сергеевна")
                        .clubName("Физика").studentsCount(10).build()
        );
        when(service.getAll()).thenReturn(mockResult);

        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Иванов Иван Иванович"))
                .andExpect(jsonPath("$[0].clubName").value("Робототехника"))
                .andExpect(jsonPath("$[0].studentsCount").value(15))
                .andExpect(jsonPath("$[1].fullName").value("Петрова Анна Сергеевна"));
    }

    @Test
    @DisplayName("GET /api/teachers/{id} — возвращает одного и 200 OK")
    void getById_returnsOne() throws Exception {
        TeacherDto mock = TeacherDto.builder()
                .id(5L).fullName("Тестов Тест Тестович")
                .clubName("Физика").studentsCount(12)
                .build();
        when(service.getById(5L)).thenReturn(mock);

        mockMvc.perform(get("/api/teachers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.fullName").value("Тестов Тест Тестович"))
                .andExpect(jsonPath("$.clubName").value("Физика"))
                .andExpect(jsonPath("$.studentsCount").value(12));
    }

    @Test
    @DisplayName("POST /api/teachers — создаёт и возвращает 201 CREATED")
    void create_returns201() throws Exception {
        TeacherDto request = TeacherDto.builder()
                .fullName("Новый Нов Нович")
                .clubName("Программирование")
                .studentsCount(20)
                .build();

        TeacherDto response = TeacherDto.builder()
                .id(100L)
                .fullName("Новый Нов Нович")
                .clubName("Программирование")
                .studentsCount(20)
                .build();

        when(service.create(any(TeacherDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.fullName").value("Новый Нов Нович"))
                .andExpect(jsonPath("$.studentsCount").value(20));
    }

    @Test
    @DisplayName("PUT /api/teachers/{id} — обновляет и возвращает 200 OK")
    void update_returns200() throws Exception {
        TeacherDto request = TeacherDto.builder()
                .fullName("Обновляев Обнов Обнович")
                .clubName("Программирование")
                .studentsCount(25)
                .build();

        TeacherDto response = TeacherDto.builder()
                .id(1L)
                .fullName("Обновляев Обнов Обнович")
                .clubName("Программирование")
                .studentsCount(25)
                .build();

        when(service.update(eq(1L), any(TeacherDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/teachers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clubName").value("Программирование"))
                .andExpect(jsonPath("$.studentsCount").value(25));
    }

    @Test
    @DisplayName("DELETE /api/teachers/{id} — удаляет и возвращает 204 NO CONTENT")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/teachers/1"))
                .andExpect(status().isNoContent());
    }
}