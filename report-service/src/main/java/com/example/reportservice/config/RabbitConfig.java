package com.example.reportservice.config;

import com.example.reportservice.messaging.ReportQueues;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Топология очереди: основной обменник + очередь с DLX и очередь «мёртвых» сообщений.
 * Всё durable — переживает перезапуск брокера.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange reportsExchange() {
        return new DirectExchange(ReportQueues.EXCHANGE, true, false);
    }

    @Bean
    public Queue reportsQueue() {
        return QueueBuilder.durable(ReportQueues.QUEUE)
                .deadLetterExchange(ReportQueues.DLX)
                .deadLetterRoutingKey(ReportQueues.DLQ)
                .build();
    }

    @Bean
    public Binding reportsBinding() {
        return BindingBuilder.bind(reportsQueue()).to(reportsExchange()).with(ReportQueues.ROUTING_KEY);
    }

    @Bean
    public DirectExchange reportsDeadLetterExchange() {
        return new DirectExchange(ReportQueues.DLX, true, false);
    }

    @Bean
    public Queue reportsDeadLetterQueue() {
        return QueueBuilder.durable(ReportQueues.DLQ).build();
    }

    @Bean
    public Binding reportsDeadLetterBinding() {
        return BindingBuilder.bind(reportsDeadLetterQueue())
                .to(reportsDeadLetterExchange())
                .with(ReportQueues.DLQ);
    }
}
