package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 0;

    @Override
    public Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User getUserById(Long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        return user;
    }

    @Override
    public User createUser(User user) {
        user.setId(++currentId);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null || !users.containsKey(user.getId())) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        users.remove(id);
    }

    @Override
    public boolean containsUser(Long id) {
        return users.containsKey(id);
    }

    @Override
    public Collection<User> getAll() {
        return List.of();
    }

    @Override
    public User getById(Long id) {
        return null;
    }

    @Override
    public User create(User user) {
        return null;
    }

    @Override
    public User update(User user) {
        return null;
    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public boolean contains(Long id) {
        return false;
    }

    @Override
    public boolean isEmailExists(String email) {
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equals(email));
    }

    @Override
    public boolean isLoginExists(String login) {
        return users.values().stream()
                .anyMatch(user -> user.getLogin().equals(login));
    }

    @Override
    public boolean isEmailExists(String email, Long excludeUserId) {
        return users.values().stream()
                .anyMatch(user -> !user.getId().equals(excludeUserId) &&
                        user.getEmail().equals(email));
    }

    @Override
    public boolean isLoginExists(String login, Long excludeUserId) {
        return users.values().stream()
                .anyMatch(user -> !user.getId().equals(excludeUserId) &&
                        user.getLogin().equals(login));
    }

    @Override
    public List<User> getUsersByIds(List<Long> ids) {
        return ids.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
