package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        User user = userStorage.getUserById(userId);
        User friend = userStorage.getUserById(friendId);

        if (userId.equals(friendId)) {
            throw new ConditionsNotMetException("Нельзя добавить себя в друзья");
        }

        if (user.getFriends().contains(friendId)) {
            return;
        }

        user.addFriend(friendId);
        friend.addFriend(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);

        log.debug("Пользователь id={} добавил в друзья пользователя id={}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        User user = userStorage.getUserById(userId);
        User friend = userStorage.getUserById(friendId);

        if (!user.getFriends().contains(friendId)) {
            return;
        }

        user.removeFriend(friendId);
        friend.removeFriend(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);

        log.debug("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ConditionsNotMetException("ID пользователя должен быть положительным числом. Получено: " + userId);
        }
    }

    public List<User> getFriends(Long userId) {
        User user = userStorage.getUserById(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            User friend = userStorage.getUserById(friendId);
            friends.add(friend);
        }
        return friends;
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {

        if (userId1.equals(userId2)) {
            throw new ConditionsNotMetException("Сам себе друг - не положено");
        }

        User user1 = userStorage.getUserById(userId1);
        User user2 = userStorage.getUserById(userId2);

        Set<Long> friends1 = user1.getFriends();
        Set<Long> friends2 = user2.getFriends();

        List<User> commonFriends = new ArrayList<>();
        for (Long friendId : friends1) {
            if (friends2.contains(friendId)) {
                User friend = userStorage.getUserById(friendId);
                commonFriends.add(friend);
            }
        }
        return commonFriends;
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id);
    }

    public User createUser(User user) {

        if (userStorage.isEmailExists(user.getEmail())) {
            throw new ConditionsNotMetException("Email уже используется");
        }

        if (userStorage.isLoginExists(user.getLogin())) {
            throw new ConditionsNotMetException("Login уже используется");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.createUser(user);
    }

    public User updateUser(User user) {

        if (!userStorage.containsUser(user.getId())) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.updateUser(user);
    }

    public boolean isEmailExists(String email) {
        return userStorage.isEmailExists(email);
    }

    public boolean isLoginExists(String login) {
        return userStorage.isLoginExists(login);
    }

    public boolean isEmailExists(String email, Long excludeUserId) {
        return userStorage.isEmailExists(email, excludeUserId);
    }

    public boolean isLoginExists(String login, Long excludeUserId) {
        return userStorage.isLoginExists(login, excludeUserId);
    }

    public boolean containsUser(Long id) {
        return userStorage.containsUser(id);
    }
}