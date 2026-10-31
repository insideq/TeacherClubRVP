package com.example.teacherrvp.dto;

import java.util.List;

/**
 * Замер параллельной сортировки на синтетическом массиве
 * при разном числе потоков.
 */
public record BenchmarkResponse(
        int size,
        int availableProcessors,
        List<Row> results) {

    public record Row(
            int threads,
            double durationMs,
            long durationNanos,
            int mergeRounds) {
    }
}
