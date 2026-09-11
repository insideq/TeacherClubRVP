package com.example.teacherrvp.service;

import com.example.teacherrvp.dto.TeacherRequest;
import com.example.teacherrvp.dto.TeacherResponse;
import com.example.teacherrvp.entity.Teacher;
import com.example.teacherrvp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository repository;

    public List<TeacherResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public TeacherResponse getById(Long id) {
        Teacher t = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Руководитель не найден: id=" + id));
        return toResponse(t);
    }

    public TeacherResponse create(TeacherRequest req) {
        Teacher t = Teacher.builder()
                .fullName(req.getFullName())
                .clubName(req.getClubName())
                .studentsCount(req.getStudentsCount())
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(repository.save(t));
    }

    public TeacherResponse update(Long id, TeacherRequest req) {
        Teacher t = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Руководитель не найден: id=" + id));
        t.setFullName(req.getFullName());
        t.setClubName(req.getClubName());
        t.setStudentsCount(req.getStudentsCount());
        return toResponse(repository.save(t));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Руководитель не найден: id=" + id);
        }
        repository.deleteById(id);
    }

    private TeacherResponse toResponse(Teacher t) {
        return TeacherResponse.builder()
                .id(t.getId())
                .fullName(t.getFullName())
                .clubName(t.getClubName())
                .studentsCount(t.getStudentsCount())
                .createdAt(t.getCreatedAt())
                .build();
    }
}