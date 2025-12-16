package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 0;

    @Override
    public Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User createUser(User user) {
        user.setId(++currentId);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        users.remove(id);
    }

    @Override
    public boolean containsUser(Long id) {
        return users.containsKey(id);
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
}
