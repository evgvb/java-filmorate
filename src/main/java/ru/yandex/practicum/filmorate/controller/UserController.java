package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Collection<User>> getUsers() {
        Collection<User> users = userService.getAllUsers();
        log.info("Запрошен список пользователей, возвращено {} пользователей", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        log.info("Запрошен пользователь с id: {}", id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<User>> getFriends(@PathVariable Long id) {
        List<User> friends = userService.getFriends(id);
        log.info("Запрошен список друзей пользователя с id: {}, найдено {} друзей", id, friends.size());
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public ResponseEntity<List<User>> getCommonFriends(
            @PathVariable Long id,
            @PathVariable Long otherId) {

        List<User> commonFriends = userService.getCommonFriends(id, otherId);
        log.info("Запрошены общие друзья пользователей {} и {}, найдено {} общих друзей",
                id, otherId, commonFriends.size());
        return ResponseEntity.ok(commonFriends);
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        User createdUser = userService.createUser(user);
        log.info("Создан новый пользователь: {}", createdUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping
    public ResponseEntity<User> update(@Valid @RequestBody User user) {
        User existingUser = userService.getUserById(user.getId());

        if (existingUser.getFriends() != null) {
            user.setFriends(existingUser.getFriends());
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        User updatedUser = userService.updateUser(user);
        log.info("Пользователь обновлен: {}", updatedUser);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> addFriend(
            @PathVariable Long id,
            @PathVariable Long friendId) {

        userService.addFriend(id, friendId);
        log.info("Пользователь {} добавил в друзья пользователя {}", id, friendId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @PathVariable Long id,
            @PathVariable Long friendId) {

        if (id == null || id <= 0) {
            throw new ConditionsNotMetException("ID пользователя должен быть положительным числом");
        }

        if (friendId == null || friendId <= 0) {
            throw new ConditionsNotMetException("ID друга должен быть положительным числом");
        }

        if (!userService.containsUser(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        if (!userService.containsUser(friendId)) {
            throw new NotFoundException("Пользователь с id=" + friendId + " не найден");
        }

        userService.removeFriend(id, friendId);

        User user = userService.getUserById(id);
        if (user.getFriends().contains(friendId)) {
            log.info("Пользователь {} удалил из друзей пользователя {}", id, friendId);
        } else {
            log.info("Пользователи {} и {} не были друзьями, операция удаления пропущена", id, friendId);
        }

        return ResponseEntity.ok().build();
    }
}