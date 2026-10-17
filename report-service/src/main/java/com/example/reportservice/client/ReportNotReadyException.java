package com.example.reportservice.client;

/**
 * Отчёт ещё не появился в файловом хранилище.
 */
public class ReportNotReadyException extends RuntimeException {

    public ReportNotReadyException(String reportId) {
        super("Отчёт " + reportId + " ещё не сохранён в файловом хранилище");
    }
}
