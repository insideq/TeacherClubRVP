package com.example.teacherrvp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDto {

    private Long id;

    @NotBlank(message = "ФИО не может быть пустым")
    @Size(max = 200, message = "ФИО не длиннее 200 символов")
    private String fullName;

    @NotBlank(message = "Название кружка не может быть пустым")
    @Size(max = 200, message = "Название кружка не длиннее 200 символов")
    private String clubName;

    @NotNull(message = "Количество обучающихся обязательно")
    @Min(value = 0, message = "Количество обучающихся не может быть отрицательным")
    private Integer studentsCount;

    @Size(max = 200, message = "Квалификация не длиннее 200 символов")
    private String qualification;

    private LocalDateTime createdAt;
}