package com.example.reportservice.messaging;

/**
 * Брокер недоступен или не подтвердил приём сообщения.
 * Отдельный тип нужен, чтобы контроллер мог ответить 503, а не «сырой» 500.
 */
public class MessagingUnavailableException extends RuntimeException {

    public MessagingUnavailableException(String message) {
        super(message);
    }

    public MessagingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
