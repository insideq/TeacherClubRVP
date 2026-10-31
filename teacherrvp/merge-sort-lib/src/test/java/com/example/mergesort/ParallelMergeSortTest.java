package com.example.mergesort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelMergeSortTest {

    @Test
    @DisplayName("Сортирует пример из задания: 11 идентификаторов в 5 потоков")
    void sortsLabExample() {
        List<Integer> ids = List.of(3, 2, 6, 9, 2, 3, 5, 8, 2, 1, 11);

        SortResult<Integer> result = ParallelMergeSort.sort(ids, Comparator.naturalOrder(), 5);

        assertEquals(List.of(1, 2, 2, 2, 3, 3, 5, 6, 8, 9, 11), result.items());
        assertEquals(11, result.size());
        assertEquals(5, result.threads());
        assertEquals(List.of(
                List.of(2, 2, 2, 2, 3),
                List.of(4, 4, 3),
                List.of(8, 3),
                List.of(11)), result.rounds());
        assertTrue(result.durationNanos() > 0);
    }

    @Test
    @DisplayName("Результат не зависит от числа потоков")
    void sameResultForDifferentThreadCounts() {
        List<Integer> data = new ArrayList<>(new Random(42)
                .ints(500, 0, 1000)
                .boxed()
                .toList());

        List<Integer> expected = new ArrayList<>(data);
        expected.sort(Comparator.naturalOrder());

        for (int threads : new int[]{1, 2, 3, 4, 8, 16, 100}) {
            SortResult<Integer> result = ParallelMergeSort.sort(data, Comparator.naturalOrder(), threads);
            assertEquals(expected, result.items(), "разошлось при threads=" + threads);
        }
    }

    @Test
    @DisplayName("Пустой список и один элемент")
    void handlesEdgeCases() {
        assertEquals(List.of(), ParallelMergeSort.sort(List.<Integer>of(), Comparator.naturalOrder(), 4).items());

        SortResult<Integer> single = ParallelMergeSort.sort(List.of(7), Comparator.naturalOrder(), 4);
        assertEquals(List.of(7), single.items());
        assertEquals(1, single.threads());
    }

    @Test
    @DisplayName("Сортирует по убыванию через reversed()")
    void sortsDescending() {
        List<Integer> data = IntStream.rangeClosed(1, 100).boxed().toList();

        SortResult<Integer> result = ParallelMergeSort.sort(
                data, Comparator.<Integer>naturalOrder().reversed(), 4);

        assertEquals(100, result.items().getFirst());
        assertEquals(1, result.items().getLast());
    }

    @Test
    @DisplayName("Исходный список не изменяется")
    void doesNotMutateSource() {
        List<Integer> data = new ArrayList<>(List.of(3, 1, 2));
        ParallelMergeSort.sort(data, Comparator.naturalOrder(), 3);
        assertEquals(List.of(3, 1, 2), data);
    }
}
