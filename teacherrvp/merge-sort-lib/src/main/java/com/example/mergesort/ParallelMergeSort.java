package com.example.mergesort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Параллельная сортировка слиянием.
 * Схема:
 *  1. массив режется на N кусков, где N — заданное число потоков;
 *  2. каждый кусок сортируется своим потоком (фаза 1);
 *  3. дальше куски попарно сливаются раундами, и каждый раунд тоже параллельный
 *     (фаза 2). При 11 элементах и 5 потоках раунды идут 5 → 3 → 2 → 1.
 * Алгоритм не зависит от домена: работает с любым списком и Comparator,
 * поэтому вынесен в отдельную библиотеку.
 */
public final class ParallelMergeSort {

    private ParallelMergeSort() {
    }

    /**
     * @param source     исходные данные
     * @param comparator порядок сортировки
     * @param threads    число первоначальных потоков (оно же число кусков)
     */
    public static <T> SortResult<T> sort(List<T> source, Comparator<T> comparator, int threads) {
        if (source == null || source.isEmpty()) {
            return new SortResult<>(List.of(), 0, 0, 0.0, 0L, List.of());
        }

        // Потоков не может быть больше, чем элементов
        int effectiveThreads = Math.max(1, Math.min(threads, source.size()));
        long startNanos = System.nanoTime();

        ExecutorService pool = Executors.newFixedThreadPool(effectiveThreads, new WorkerThreadFactory());
        try {
            List<List<T>> chunks = split(source, effectiveThreads);
            List<List<Integer>> rounds = new ArrayList<>();
            rounds.add(sizes(chunks));

            // Фаза 1: каждый кусок сортируется в своём потоке
            List<CompletableFuture<List<T>>> sortedChunks = new ArrayList<>(chunks.size());
            for (List<T> chunk : chunks) {
                sortedChunks.add(CompletableFuture.supplyAsync(() -> {
                    List<T> copy = new ArrayList<>(chunk);
                    copy.sort(comparator);
                    return copy;
                }, pool));
            }
            List<List<T>> level = joinAll(sortedChunks);

            // Фаза 2: попарное слияние раундами, каждый раунд — параллельно
            while (level.size() > 1) {
                List<CompletableFuture<List<T>>> merged = new ArrayList<>();
                for (int i = 0; i < level.size(); i += 2) {
                    if (i + 1 < level.size()) {
                        List<T> left = level.get(i);
                        List<T> right = level.get(i + 1);
                        merged.add(CompletableFuture.supplyAsync(
                                () -> merge(left, right, comparator), pool));
                    } else {
                        // нечётный кусок переходит в следующий раунд без слияния
                        merged.add(CompletableFuture.completedFuture(level.get(i)));
                    }
                }
                level = joinAll(merged);
                rounds.add(sizes(level));
            }

            long durationNanos = System.nanoTime() - startNanos;
            return new SortResult<>(
                    level.getFirst(),
                    source.size(),
                    effectiveThreads,
                    durationNanos / 1_000_000.0,
                    durationNanos,
                    rounds);
        } finally {
            pool.shutdown();
        }
    }

    private static <T> List<List<T>> joinAll(List<CompletableFuture<List<T>>> futures) {
        List<List<T>> result = new ArrayList<>(futures.size());
        for (CompletableFuture<List<T>> future : futures) {
            result.add(future.join());
        }
        return result;
    }

    /**
     * Режет список на parts частей. «Лишние» элементы уходят в последние куски,
     * поэтому 11 на 5 частей даёт [2,2,2,2,3] — как в примере из задания.
     */
    private static <T> List<List<T>> split(List<T> source, int parts) {
        int size = source.size();
        int base = size / parts;
        int extra = size % parts;
        List<List<T>> chunks = new ArrayList<>(parts);

        int index = 0;
        for (int i = 0; i < parts; i++) {
            int length = base + (i >= parts - extra ? 1 : 0);
            chunks.add(new ArrayList<>(source.subList(index, index + length)));
            index += length;
        }
        return chunks;
    }

    private static <T> List<Integer> sizes(List<List<T>> chunks) {
        return chunks.stream().map(List::size).toList();
    }

    /** Классическое слияние двух отсортированных списков. */
    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<T> comparator) {
        List<T> result = new ArrayList<>(left.size() + right.size());
        int i = 0;
        int j = 0;

        while (i < left.size() && j < right.size()) {
            if (comparator.compare(left.get(i), right.get(j)) <= 0) {
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }
        while (i < left.size()) {
            result.add(left.get(i++));
        }
        while (j < right.size()) {
            result.add(right.get(j++));
        }
        return result;
    }

    /** Именует рабочие потоки, чтобы их было видно в логах и дампах потоков. */
    private static final class WorkerThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "merge-sort-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
