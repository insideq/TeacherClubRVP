package com.example.teacherrvp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequest {

    @NotBlank(message = "ФИО не может быть пустым")
    @Size(max = 200)
    private String fullName;

    @NotBlank(message = "Название кружка не может быть пустым")
    @Size(max = 200)
    private String clubName;

    @NotNull(message = "Количество обучающихся обязательно")
    @Min(value = 0, message = "Количество обучающихся не может быть отрицательным")
    private Integer studentsCount;
}