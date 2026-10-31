package com.example.authservice.exception;

/**
 * Попытка зарегистрировать уже существующий логин.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String username) {
        super("Пользователь '" + username + "' уже существует");
    }
}
