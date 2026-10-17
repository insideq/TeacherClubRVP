package com.example.reportservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Публикация отчёта в очередь.
 * Транзакционность на стороне продюсера: сообщение помечается persistent и мы
 * ДОЖИДАЕМСЯ подтверждения брокера (publisher confirm). Если подтверждения нет —
 * бросаем исключение, и клиент получит ошибку вместо ложного «всё хорошо».
 * JSON сериализуем сами: так сообщение не тащит __TypeId__ с именем класса продюсера,
 * и консьюмер в другом сервисе не зависит от пакетов продюсера.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${queue.reports.confirm-timeout-seconds:5}")
    private long confirmTimeoutSeconds;

    public void publish(ReportMessage message) {
        String payload = toJson(message);

        try {
            CorrelationData correlation = new CorrelationData(message.reportId());
            rabbitTemplate.convertAndSend(
                    ReportQueues.EXCHANGE,
                    ReportQueues.ROUTING_KEY,
                    payload,
                    rabbitMessage -> {
                        MessageProperties props = rabbitMessage.getMessageProperties();
                        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        props.setContentEncoding(StandardCharsets.UTF_8.name());
                        props.setMessageId(message.reportId());
                        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return rabbitMessage;
                    },
                    correlation);

            awaitConfirm(correlation, message.reportId());
            log.info("Отчёт {} опубликован в очередь {} (брокер подтвердил приём)",
                    message.reportId(), ReportQueues.QUEUE);
        } catch (org.springframework.amqp.AmqpException e) {
            log.error("Не удалось опубликовать отчёт {}: {}", message.reportId(), e.getMessage(), e);
            throw new MessagingUnavailableException(
                    "RabbitMQ недоступен: " + e.getMessage(), e);
        }
    }

    private void awaitConfirm(CorrelationData correlation, String reportId) {
        try {
            CorrelationData.Confirm confirm =
                    correlation.getFuture().get(confirmTimeoutSeconds, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new MessagingUnavailableException(
                        "Брокер отклонил сообщение " + reportId + ": " + confirm.getReason());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingUnavailableException("Публикация отчёта " + reportId + " прервана", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new MessagingUnavailableException(
                    "Брокер не подтвердил приём отчёта " + reportId
                            + " за " + confirmTimeoutSeconds + " c — сообщение не отправлено", e);
        }
    }

    private String toJson(ReportMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать сообщение отчёта", e);
        }
    }
}
