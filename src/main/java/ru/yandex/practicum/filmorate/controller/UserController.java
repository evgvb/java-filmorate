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
        log.info("Запрошен список пользователей, возвращено {} пользователей", users.size());
        return ResponseEntity.ok(users.values());
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        boolean loginExists = users.values().stream()
                .anyMatch(existingUser -> existingUser.getLogin().equals(user.getLogin()));
        if (loginExists) {
            log.info("Этот login уже используется другим пользователем id: {}", user.getId());
            throw new ConditionsNotMetException("Этот login уже используется другим пользователем login: " + user.getLogin());
        }

        boolean emailExists = users.values().stream()
                .anyMatch(existingUser -> existingUser.getEmail().equals(user.getEmail()));
        if (emailExists) {
            log.info("Этот email уже используется другим пользователем id: {}", user.getId());
            throw new ConditionsNotMetException("Этот email уже используется другим пользователем email: " + user.getEmail());
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Создан новый пользователь: {}", user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody User user) {
        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.warn("Пользователь не найден id: {}", user.getId());
            throw new NotFoundException("Пользователь не найден id: " + user.getId());
        }

        boolean loginExists = users.values().stream()
                .anyMatch(existingUser ->
                        !existingUser.getId().equals(user.getId()) &&
                                existingUser.getLogin().equals(user.getLogin())
                );
        if (loginExists) {
            log.warn("Этот login уже используется другим пользователем id: {}", user.getLogin());
            throw new ConditionsNotMetException("Этот login уже используется другим пользователем: " + user.getLogin());
        }

        boolean emailExists = users.values().stream()
                .anyMatch(existingUser ->
                        !existingUser.getId().equals(user.getId()) &&
                                existingUser.getEmail().equals(user.getEmail())
                );
        if (emailExists) {
            log.warn("Этот email уже используется другим пользователем: {}", user.getEmail());
            throw new ConditionsNotMetException("Этот email уже используется другим пользователем: " + user.getEmail());
        }

        User updatedUser = users.get(user.getId());

        // Обновляем все поля
        if (user.getEmail() != null && !user.getEmail().isBlank() &&
                user.getEmail().matches("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")) {
            updatedUser.setEmail(user.getEmail());
        }
        boolean isUpdateLogin = false;
        //а может не надо менять name
        //boolean isLoginEqName = updatedUser.getLogin().equals(updatedUser.getName());
        if (user.getLogin() != null && !user.getLogin().isBlank() && !user.getLogin().contains(" ")) {
            updatedUser.setLogin(user.getLogin());
            isUpdateLogin = true;
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            updatedUser.setName(user.getName());
        } else if (isUpdateLogin) {
            // Если имя не указано и login поменялся, используем логин.
            // ??? а если updatedUser.name был не пустой, то может и не надо его менять...
            // как вариант: менять если он был равен старому login...
            //} else if (isUpdateLogin && isLoginEqName) {
            updatedUser.setName(user.getLogin());
        }
        if (user.getBirthday() != null && !user.getBirthday().isAfter(LocalDate.now())) {
            updatedUser.setBirthday(user.getBirthday());
        }

        log.info("Пользователь обновлен: {}", updatedUser);
        return ResponseEntity.ok(updatedUser);
    }
}