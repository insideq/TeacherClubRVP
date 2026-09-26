package com.example.reportservice.controller;

import com.example.reportservice.dto.ReportResponse;
import com.example.reportservice.dto.TeacherReportRow;
import com.example.reportservice.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@DisplayName("ReportController — позитивные тесты")
class ReportControllerPositiveTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService service;

    @Test
    @DisplayName("GET /api/reports/teachers — возвращает отчёт с суммой по руководителям")
    void report_returnsAggregatedData() throws Exception {
        ReportResponse mock = ReportResponse.builder()
                .title("Отчёт: количество обучающихся у каждого руководителя")
                .generatedAt(LocalDateTime.now())
                .rows(List.of(
                        TeacherReportRow.builder().fullName("Иванов Иван Иванович").totalStudents(28L).build(),
                        TeacherReportRow.builder().fullName("Петрова Анна Сергеевна").totalStudents(22L).build()
                ))
                .grandTotal(50L)
                .teachersCount(2)
                .build();

        when(service.buildTeacherReport()).thenReturn(mock);

        mockMvc.perform(get("/api/reports/teachers"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Отчёт: количество обучающихся у каждого руководителя"))
                .andExpect(jsonPath("$.grandTotal").value(50))
                .andExpect(jsonPath("$.teachersCount").value(2))
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andExpect(jsonPath("$.rows[0].fullName").value("Иванов Иван Иванович"))
                .andExpect(jsonPath("$.rows[0].totalStudents").value(28))
                .andExpect(jsonPath("$.rows[1].fullName").value("Петрова Анна Сергеевна"))
                .andExpect(jsonPath("$.rows[1].totalStudents").value(22));
    }

    @Test
    @DisplayName("GET /api/reports/teachers — возвращает пустой отчёт, если данных нет")
    void report_empty_returnsEmptyResponse() throws Exception {
        ReportResponse empty = ReportResponse.builder()
                .title("Отчёт: количество обучающихся у каждого руководителя")
                .generatedAt(LocalDateTime.now())
                .rows(List.of())
                .grandTotal(0L)
                .teachersCount(0)
                .build();

        when(service.buildTeacherReport()).thenReturn(empty);

        mockMvc.perform(get("/api/reports/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(0))
                .andExpect(jsonPath("$.grandTotal").value(0))
                .andExpect(jsonPath("$.teachersCount").value(0));
    }
}