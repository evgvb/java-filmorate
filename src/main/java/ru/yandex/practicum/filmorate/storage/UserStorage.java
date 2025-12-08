package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;

public interface UserStorage {
    Collection<User> getAll();

    User getById(Long id);

    User create(User user);

    User update(User user);

    void delete(Long id);

    boolean contains(Long id);

    boolean isEmailExists(String email);

    boolean isLoginExists(String login);

    boolean isEmailExists(String email, Long excludeUserId);

    boolean isLoginExists(String login, Long excludeUserId);

    List<User> getUsersByIds(List<Long> ids);

    Collection<User> getAllUsers();

    User getUserById(Long id);

    User updateUser(User user);

    User createUser(User user);

    void deleteUser(Long id);

    boolean containsUser(Long id);
}
