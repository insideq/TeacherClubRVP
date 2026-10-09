package com.example.reportservice.client;

import com.example.reportservice.dto.TeacherDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherServiceClient {

    private final RestTemplate restTemplate;

    @Value("${teacher-service.url}")
    private String teacherServiceUrl;

    public List<TeacherDto> getAllTeachers() {
        String url = teacherServiceUrl + "/api/teachers";
        log.info("Calling teacher-service: {}", url);

        ResponseEntity<List<TeacherDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody() != null ? response.getBody() : List.of();
    }
}