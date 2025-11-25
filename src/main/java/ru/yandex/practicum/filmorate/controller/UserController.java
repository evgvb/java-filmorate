package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final Map<Long, User> users = new HashMap<>();

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @GetMapping
    public ResponseEntity<Collection<User>> getUsers() {
        return ResponseEntity.ok(users.values());
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        boolean emailExists = users.values().stream()
                .anyMatch(existingUser -> existingUser.getEmail().equals(user.getEmail()));
        if (emailExists) {
            log.info("Этот email уже используется id: {}", user.getId() + " email: " + user.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Создан новый пользователь: {}", user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping
    public ResponseEntity<?> update(@Valid @RequestBody User user) {
        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.warn("Пользователь не найден id: {}", user.getId());
            throw new NotFoundException("Пользователь не найден id: " + user.getId());
        }

        boolean emailExists = users.values().stream()
                .anyMatch(existingUser -> existingUser.getEmail().equals(user.getEmail()));
        if (emailExists) {
            log.warn("Этот email уже используется другим пользователем: {}", user.getEmail());
            throw new ConditionsNotMetException("Этот email уже используется другим пользователем: " + user.getEmail());
        }

        User updatedUser = users.get(user.getId());

        // Обновляем все поля
        if (user.getEmail() != null) {
            updatedUser.setEmail(user.getEmail());
        }
        if (user.getLogin() != null && !user.getLogin().isBlank()) {
            updatedUser.setLogin(user.getLogin());
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            updatedUser.setName(user.getName());
        } else {
            // Если имя не указано, используем логин
            updatedUser.setName(user.getLogin());
        }
        if (user.getBirthday() != null && !user.getBirthday().isAfter(LocalDate.now())) {
            updatedUser.setBirthday(user.getBirthday());
        }

        log.info("Пользователь обновлен: {}", updatedUser);
        return ResponseEntity.ok(updatedUser);
    }
}