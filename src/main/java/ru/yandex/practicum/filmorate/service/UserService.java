package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        getUserOrThrow(userId);
        getUserOrThrow(friendId);

        verificationFriend(userId, friendId);

        if (userStorage.hasFriend(userId, friendId)) {
            log.debug("Пользователи id={} и id={} уже являются друзьями", userId, friendId);
            return;
        }

        // проверка, user добавлен в друзья freiend
        boolean hasReciprocalRequest = userStorage.hasFriend(friendId, userId);
        User.FriendshipStatus status = hasReciprocalRequest ?
                User.FriendshipStatus.CONFIRMED : User.FriendshipStatus.UNCONFIRMED;

        userStorage.addFriend(userId, friendId, status);

        if (hasReciprocalRequest) {
            userStorage.updateFriendshipStatus(friendId, userId, User.FriendshipStatus.CONFIRMED);
        }

        log.debug("Пользователь id={} добавил в друзья пользователя id={} (статус: {})",
                userId, friendId, status);
    }

    public void removeFriend(Long userId, Long friendId) {

        validateUserId(userId);
        validateUserId(friendId);

        getUserOrThrow(userId);
        getUserOrThrow(friendId);

        verificationFriend(userId, friendId);

        if (!userStorage.hasFriend(userId, friendId)) {
            log.debug("У пользователя id={} нет друга id={}", userId, friendId);
            return;
        }

        userStorage.removeFriend(userId, friendId);

        // Если дружба была подтвержденной, у друга меняем статус на UNCONFIRMED
        if (userStorage.hasFriend(friendId, userId)) {
            userStorage.updateFriendshipStatus(friendId, userId, User.FriendshipStatus.UNCONFIRMED);
        }

        log.debug("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ConditionsNotMetException("ID пользователя должен быть положительным числом. Получено: " + userId);
        }
    }

    public List<User> getFriends(Long userId) {
        getUserOrThrow(userId);
        return userStorage.getFriendsList(userId);
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        verificationFriend(userId1, userId2);

        getUserOrThrow(userId1);
        getUserOrThrow(userId2);

        List<Long> friends1 = userStorage.getFriends(userId1);
        List<Long> friends2 = userStorage.getFriends(userId2);

        // Находим пересечение списков ID друзей
        List<Long> commonFriendIds = friends1.stream()
                .filter(friends2::contains)
                .collect(Collectors.toList());

        return userStorage.getUsersByIds(commonFriendIds);
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User getUserById(Long id) {
        return getUserOrThrow(id);
    }

    public User createUser(User user) {
        verificationLoginMail(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        User existingUser = getUserOrThrow(user.getId());

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        verificationLoginMail(user);

        return userStorage.updateUser(user);
    }

    private void verificationLoginMail(User user) {
        if (userStorage.isEmailExists(user.getEmail(), user.getId())) { //if (userStorage.isEmailExists(user.getEmail())) {
            throw new ConditionsNotMetException("Email уже используется");
        }

        if (userStorage.isLoginExists(user.getLogin(), user.getId())) { //if (userStorage.isLoginExists(user.getLogin())) {
            throw new ConditionsNotMetException("Login уже используется");
        }
    }

    private void verificationFriend(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new ConditionsNotMetException("Сам себе друг - не положено");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}