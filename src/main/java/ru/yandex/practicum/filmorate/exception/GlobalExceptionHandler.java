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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 400 - Ошибка валидации
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, String> response = new HashMap<>();
        response.put("error", "Validation error");
        response.put("message", "Ошибка валидации полей");
        response.put("details", errors.toString());

        log.warn("Ошибка валидации: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400
    }

    // 400 - Ошибка в теле запроса
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", "Ошибка в теле запроса");

        log.warn("Ошибка чтения тела запроса: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400
    }

    // 400 - Условия не выполнены
    @ExceptionHandler(ConditionsNotMetException.class)
    public ResponseEntity<Map<String, String>> handleConditionsNotMet(ConditionsNotMetException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Validation error");
        response.put("message", ex.getMessage());

        log.warn("Ошибка условий: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400
    }

    // 404 - Ресурс не найден
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        log.warn("Ресурс не найден: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // HTTP 404
    }

    // 500 - Внутренняя ошибка сервера
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "Внутренняя ошибка сервера");

        log.error("Внутренняя ошибка сервера: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // HTTP 500
    }
}