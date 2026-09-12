package com.example.teacherrvp.service;

import com.example.teacherrvp.dto.TeacherDto;
import com.example.teacherrvp.entity.Teacher;
import com.example.teacherrvp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository repository;

    public List<TeacherDto> getAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public TeacherDto getById(Long id) {
        Teacher t = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Руководитель не найден: id=" + id));
        return toDto(t);
    }

    public TeacherDto create(TeacherDto req) {
        Teacher t = Teacher.builder()
                .fullName(req.getFullName())
                .clubName(req.getClubName())
                .studentsCount(req.getStudentsCount())
                .qualification(req.getQualification())
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(repository.save(t));
    }

    public TeacherDto update(Long id, TeacherDto req) {
        Teacher t = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Руководитель не найден: id=" + id));
        t.setFullName(req.getFullName());
        t.setClubName(req.getClubName());
        t.setStudentsCount(req.getStudentsCount());
        t.setQualification(req.getQualification());
        return toDto(repository.save(t));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Руководитель не найден: id=" + id);
        }
        repository.deleteById(id);
    }

    private TeacherDto toDto(Teacher t) {
        return TeacherDto.builder()
                .id(t.getId())
                .fullName(t.getFullName())
                .clubName(t.getClubName())
                .studentsCount(t.getStudentsCount())
                .qualification(t.getQualification())
                .createdAt(t.getCreatedAt())
                .build();
    }
}