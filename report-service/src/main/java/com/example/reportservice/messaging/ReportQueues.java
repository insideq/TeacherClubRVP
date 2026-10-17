package com.example.reportservice.messaging;

/**
 * Имена объектов в RabbitMQ. Те же значения объявлены и в file-storage-service —
 * топологию объявляют оба сервиса, это идемпотентно и позволяет им стартовать в любом порядке.
 */
public final class ReportQueues {

    /** Основной обменник, куда публикует report-service. */
    public static final String EXCHANGE = "reports.exchange";

    /** Очередь, из которой читает file-storage-service. */
    public static final String QUEUE = "reports.queue";

    /** Ключ маршрутизации для сообщений с отчётом. */
    public static final String ROUTING_KEY = "report.csv";

    /** Обменник «мёртвых» сообщений. */
    public static final String DLX = "reports.dlx";

    /** Очередь «мёртвых» сообщений: сюда попадает всё, что не удалось обработать. */
    public static final String DLQ = "reports.dlq";

    private ReportQueues() {
    }
}
