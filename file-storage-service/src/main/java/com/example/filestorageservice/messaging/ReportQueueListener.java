package com.example.filestorageservice.messaging;

import com.example.filestorageservice.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Потребитель очереди отчётов: складывает CSV в файловое хранилище.
 *
 * Транзакционность на стороне консьюмера:
 *  - ack вручную и ТОЛЬКО после успешной записи в MinIO; если процесс упадёт раньше,
 *    сообщение останется неподтверждённым и вернётся в очередь;
 *  - при ошибке basicNack(requeue=false) — сообщение уходит в DLQ, а не теряется
 *    и не крутится в бесконечном цикле;
 *  - идемпотентность: ключ объекта = reportId, поэтому повторная доставка не создаёт дубль.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportQueueListener {

    private static final String CSV_CONTENT_TYPE = "text/csv";

    private final FileStorageService storage;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = ReportQueues.QUEUE, ackMode = "MANUAL")
    public void onReport(String payload,
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        ReportMessage message;
        try {
            message = objectMapper.readValue(payload, ReportMessage.class);
        } catch (Exception e) {
            log.error("Некорректное сообщение, отправляем в {}: {}", ReportQueues.DLQ, payload, e);
            reject(channel, deliveryTag);
            return;
        }

        String objectKey = message.objectKey();
        try {
            if (storage.exists(objectKey)) {
                log.info("Отчёт {} уже сохранён как {} — повторная доставка, пропускаем",
                        message.reportId(), objectKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            storage.storeText(objectKey, message.csv(), CSV_CONTENT_TYPE);

            // Подтверждаем брокеру уже ПОСЛЕ того, как файл реально лёг в MinIO.
            channel.basicAck(deliveryTag, false);
            log.info("Отчёт {} сохранён как {} ({} символов)",
                    message.reportId(), objectKey, message.csv().length());
        } catch (Exception e) {
            log.error("Не удалось сохранить отчёт {} — сообщение уйдёт в {}",
                    message.reportId(), ReportQueues.DLQ, e);
            reject(channel, deliveryTag);
        }
    }

    /** requeue=false: сообщение не будет крутиться в цикле, а уедет в DLQ. */
    private void reject(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error("Не удалось отклонить сообщение", e);
        }
    }
}
