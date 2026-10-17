package com.example.reportservice.client;

/**
 * Файловое хранилище недоступно.
 */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
