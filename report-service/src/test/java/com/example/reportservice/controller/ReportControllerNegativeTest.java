package com.example.reportservice.controller;

import com.example.reportservice.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@DisplayName("ReportController — негативные тесты")
class ReportControllerNegativeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService service;

    @Test
    @DisplayName("GET /api/reports/teachers — 500, если сервис бросил исключение")
    void report_internalError_returns500() throws Exception {
        when(service.buildTeacherReport())
                .thenThrow(new RuntimeException("БД недоступна"));

        mockMvc.perform(get("/api/reports/teachers"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("БД недоступна"));
    }

    @Test
    @DisplayName("GET /api/reports/unknown — 404")
    void unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/reports/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/reports/teachers — 405 Method Not Allowed")
    void post_returns405() throws Exception {
        mockMvc.perform(post("/api/reports/teachers"))
                .andExpect(status().isMethodNotAllowed());
    }
}