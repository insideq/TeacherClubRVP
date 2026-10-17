package com.example.reportservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Асинхронный клиент файлового хранилища на CompletableFuture.
 * Каждый вызов уходит в отдельный пул потоков ({@code storageExecutor}), поэтому
 * несколько запросов выполняются параллельно, а результат можно склеивать
 * через thenCombine / thenCompose, не блокируя поток обработки запроса.
 */
@Component
@Slf4j
public class AsyncStorageClient {

    private static final long CALL_TIMEOUT_SECONDS = 5;
    private static final long POLL_INTERVAL_MS = 300;

    private final RestTemplate restTemplate;
    private final Executor executor;
    private final String baseUrl;

    public AsyncStorageClient(RestTemplate restTemplate,
                              @Qualifier("storageExecutor") Executor executor,
                              @Value("${file-storage.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.executor = executor;
        this.baseUrl = baseUrl;
    }

    /**
     * Асинхронно забирает CSV отчёта из хранилища.
     * orTimeout страхует от «зависших» вызовов: дольше таймаута не ждём.
     */
    public CompletableFuture<byte[]> fetchContent(String reportId) {
        log.info("Fetching CONTENT for {}", reportId);
        return CompletableFuture
                .supplyAsync(() -> download(reportId), executor)
                .orTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Асинхронно забирает метаданные объекта (размер и дату изменения).
     */
    public CompletableFuture<StoredFileInfo> fetchInfo(String reportId) {
        log.info("Fetching INFO for {}", reportId);
        return CompletableFuture
                .supplyAsync(() -> findInfo(reportId), executor)
                .orTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Ждёт появления отчёта: повторяет запрос с паузой, пока файл не найдётся
     * или не истечёт таймаут. Всё это асинхронно — поток запроса не блокируется.
     */
    public CompletableFuture<byte[]> awaitContent(String reportId, Duration timeout) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        return attempt(reportId, deadlineNanos);
    }

    private CompletableFuture<byte[]> attempt(String reportId, long deadlineNanos) {
        return fetchContent(reportId).exceptionallyCompose(failure -> {
            boolean notReady = unwrap(failure) instanceof ReportNotReadyException;
            if (notReady && System.nanoTime() < deadlineNanos) {
                log.info("Отчёт {} ещё не готов, повтор через {} мс", reportId, POLL_INTERVAL_MS);
                return CompletableFuture
                        .supplyAsync(() -> null,
                                CompletableFuture.delayedExecutor(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS))
                        .thenCompose(ignored -> attempt(reportId, deadlineNanos));
            }
            return CompletableFuture.failedFuture(failure);
        });
    }

    private byte[] download(String reportId) {
        log.info("START CONTENT for {}", reportId);
        String url = baseUrl + "/api/files/" + reportId + ".csv";
        try {
            return restTemplate.getForObject(url, byte[].class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ReportNotReadyException(reportId);
        } catch (RestClientException e) {
            throw new StorageUnavailableException("Файловое хранилище недоступно: " + e.getMessage(), e);
        }
    }

    private StoredFileInfo findInfo(String reportId) {
        log.info("START INFO for {}", reportId);
        String objectKey = reportId + ".csv";
        try {
            StoredFileInfo[] files = restTemplate.getForObject(baseUrl + "/api/files", StoredFileInfo[].class);
            if (files == null) {
                throw new ReportNotReadyException(reportId);
            }
            return Arrays.stream(files)
                    .filter(file -> objectKey.equals(file.key()))
                    .findFirst()
                    .orElseThrow(() -> new ReportNotReadyException(reportId));
        } catch (RestClientException e) {
            throw new StorageUnavailableException("Файловое хранилище недоступно: " + e.getMessage(), e);
        }
    }

    /** Достаёт настоящую причину из обёрток CompletionException / ExecutionException. */
    public static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
