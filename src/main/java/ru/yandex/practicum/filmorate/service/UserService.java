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

        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        User friend = userStorage.getUserById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + friendId + " не найден"));

        verificationFriend(userId, friendId);

        if (user.getFriends().contains(friendId)) {
            log.debug("Пользователи id={} и id={} уже являются друзья", userId, friendId);
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

        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        User friend = userStorage.getUserById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + friendId + " не найден"));

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
        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        List<User> friends = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            User friend = userStorage.getUserById(friendId)
                    .orElseThrow(() -> new NotFoundException("Друг с id=" + friendId + " не найден"));
            friends.add(friend);
        }
        return friends;
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {

        verificationFriend(userId1, userId2);

        User user1 = userStorage.getUserById(userId1)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId1 + " не найден"));
        User user2 =  userStorage.getUserById(userId2)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId2 + " не найден"));

        Set<Long> friends1 = user1.getFriends();
        Set<Long> friends2 = user2.getFriends();

        List<User> commonFriends = new ArrayList<>();
        for (Long friendId : friends1) {
            if (friends2.contains(friendId)) {
                User friend = userStorage.getUserById(friendId)
                        .orElseThrow(() -> new NotFoundException("Друг с id=" + friendId + " не найден"));
                commonFriends.add(friend);
            }
        }
        return commonFriends;
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User getUserById(Long id) {

        return userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public User createUser(User user) {
        verificationLoginMail(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.createUser(user);
    }

    public User updateUser(User user) {

        User existingUser = userStorage.getUserById(user.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + user.getId() + " не найден"));

        if (existingUser.getFriends() != null) {
            user.setFriends(existingUser.getFriends());
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        verificationLoginMail(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.updateUser(user);
    }
//
//    public boolean isEmailExists(String email) {
//        return userStorage.isEmailExists(email);
//    }
//
//    public boolean isLoginExists(String login) {
//        return userStorage.isLoginExists(login);
//    }
//
//    public boolean isEmailExists(String email, Long excludeUserId) {
//        return userStorage.isEmailExists(email, excludeUserId);
//    }
//
//    public boolean isLoginExists(String login, Long excludeUserId) {
//        return userStorage.isLoginExists(login, excludeUserId);
//    }
//
//    public void deleteUser(Long id) {
//        if (id == null || id <= 0) {
//            throw new ConditionsNotMetException("ID пользователя должен быть положительным числом");
//        }
//
//        if (!userStorage.containsUser(id)) {
//            throw new NotFoundException("Пользователь с id=" + id + " не найден");
//        }
//
//        userStorage.deleteUser(id);
//        log.info("Удален пользователь с id: {}", id);
//    }
//
//    public boolean containsUser(Long id) {
//        return userStorage.containsUser(id);
//    }

    private void verificationLoginMail(User user) {
        if (userStorage.isEmailExists(user.getEmail())) {
            throw new ConditionsNotMetException("Email уже используется");
        }

        if (userStorage.isLoginExists(user.getLogin())) {
            throw new ConditionsNotMetException("Login уже используется");
        }
    }

    private void verificationFriend(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new ConditionsNotMetException("Сам себе друг - не положено");
        }
    }
}