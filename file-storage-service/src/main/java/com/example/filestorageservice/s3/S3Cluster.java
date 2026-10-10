package com.example.filestorageservice.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Свой отказоустойчивый клиент к кластеру MinIO.
 *
 * В распределённом MinIO все ноды равнозначны: любую операцию можно выполнить
 * на любой из них, нода сама сходит к соседям за нужными шардами. Поэтому
 * вместо внешнего балансировщика ходим по нодам по кругу, а при сетевой ошибке
 * или 5xx пробуем следующую — та же идея health-aware выбора, что и в реестре гейтвея.
 */
public class S3Cluster {

    private static final Logger log = LoggerFactory.getLogger(S3Cluster.class);

    private final List<S3Client> nodes;
    private final AtomicInteger cursor = new AtomicInteger();

    public S3Cluster(List<S3Client> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Не задан ни один endpoint MinIO (storage.s3.endpoints)");
        }
        this.nodes = List.copyOf(nodes);
        log.info("S3 cluster client initialised with {} node(s)", nodes.size());
    }

    /**
     * Выполняет операцию на очередной ноде. Если нода не ответила —
     * пробует следующую, и так по кругу до первой успешной.
     */
    public <T> T execute(Function<S3Client, T> operation) {
        int start = Math.floorMod(cursor.getAndIncrement(), nodes.size());
        SdkException lastFailure = null;

        for (int attempt = 0; attempt < nodes.size(); attempt++) {
            int index = (start + attempt) % nodes.size();
            try {
                return operation.apply(nodes.get(index));
            } catch (SdkException e) {
                if (!isRetryable(e)) {
                    throw e;
                }
                lastFailure = e;
                log.warn("MinIO node #{} did not answer ({}), trying next node", index, e.getMessage());
            }
        }
        throw lastFailure;
    }

    public int nodeCount() {
        return nodes.size();
    }

    /**
     * Повторяем только то, что имеет смысл повторять: сетевые сбои (SdkClientException)
     * и серверные ошибки 5xx. Ошибки вида 404 повторять нельзя — namespace у кластера
     * общий, значит объекта действительно нет, и обход нод ничего не изменит.
     */
    private boolean isRetryable(SdkException e) {
        if (e instanceof SdkClientException) {
            return true;
        }
        return e instanceof S3Exception s3 && s3.statusCode() >= 500;
    }
}
