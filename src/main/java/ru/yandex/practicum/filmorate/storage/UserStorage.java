package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {

    boolean isEmailExists(String email);

    boolean isLoginExists(String login);

    boolean isEmailExists(String email, Long excludeUserId);

    boolean isLoginExists(String login, Long excludeUserId);

    Collection<User> getAllUsers();

    Optional<User> getUserById(Long id);

    User updateUser(User user);

    User createUser(User user);

    void deleteUser(Long id);

    boolean containsUser(Long id);
}
