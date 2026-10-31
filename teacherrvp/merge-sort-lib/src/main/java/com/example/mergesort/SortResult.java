package com.example.mergesort;

import java.util.List;

/**
 * Результат параллельной сортировки.
 * rounds — размеры кусков после каждого раунда слияния.
 * Для примера из задания (11 идентификаторов, 5 потоков) получится
 * [[2,2,2,2,3], [4,4,3], [8,3], [11]], что соответствует схеме
 * «5 потоков → 3 → 2 → 1».
 */
public record SortResult<T>(
        List<T> items,
        int size,
        int threads,
        double durationMs,
        long durationNanos,
        List<List<Integer>> rounds) {
}
