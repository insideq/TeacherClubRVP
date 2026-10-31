package com.example.teacherrvp.dto;

import java.util.List;

/**
 * Ответ на запрос отсортированных данных пользователя.
 * Помимо самого списка отдаём время сортировки и раунды слияния —
 * по ним видно, как отработал параллельный алгоритм.
 */
public record SortedTeachersResponse(
        List<TeacherDto> items,
        int size,
        int threads,
        double durationMs,
        long durationNanos,
        List<List<Integer>> rounds) {
}
