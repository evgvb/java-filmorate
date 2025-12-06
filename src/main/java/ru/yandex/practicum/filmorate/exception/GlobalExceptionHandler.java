package ru.yandex.practicum.filmorate.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice  // Глобальный обработчик исключений для всех контроллеров
@Slf4j                 // Автоматическое создание логгера через Lombok
public class GlobalExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> response = new HashMap<>();

        // Анализ сообщения об ошибке для более точного ответа
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("Required request body is missing")) {
                response.put("error", "Bad Request");
                response.put("message", "Тело запроса обязательно"); // Отсутствует тело запроса
            } else if (message.contains("JSON parse error")) {
                response.put("error", "Bad Request");
                response.put("message", "Невалидный JSON в теле запроса"); // Неправильный JSON формат
            } else {
                response.put("error", "Bad Request");
                response.put("message", "Ошибка чтения тела запроса"); // Другие ошибки чтения
            }
        } else {
            response.put("error", "Bad Request");
            response.put("message", "Ошибка в теле запроса");
        }

        log.warn("Ошибка чтения тела запроса: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Сбор всех ошибок валидации по полям
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();    // Имя поля с ошибкой
            String errorMessage = error.getDefaultMessage();       // Сообщение об ошибке
            errors.put(fieldName, errorMessage);
        });

        // Формирование ответа
        Map<String, String> response = new HashMap<>();
        response.put("error", "Validation error");
        response.put("message", "Ошибка валидации полей");
        response.put("details", errors.toString()); // Детали по всем полям

        log.warn("Ошибка валидации: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400
    }

    @ExceptionHandler(ConditionsNotMetException.class)
    public ResponseEntity<Map<String, String>> handleConditionsNotMet(ConditionsNotMetException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Validation error");
        response.put("message", ex.getMessage()); // Используем сообщение из исключения

        log.warn("Ошибка условий: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "Внутренняя ошибка сервера"); // Общее сообщение для клиента

        // Логируем полный стектрейс для диагностики
        log.error("Внутренняя ошибка сервера: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // HTTP 500
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        log.warn("Ресурс не найден: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // HTTP 404
    }
}