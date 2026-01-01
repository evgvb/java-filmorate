package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        verificationFriend(userId, friendId);

        if (user.getFriends().contains(friendId)) {
            log.debug("Пользователи id={} и id={} уже являются друзья", userId, friendId);
            return;
        }

        // проверка, user добавлен в друзья freiend
        boolean hasReciprocalRequest = friend.getFriends().contains(userId);
        User.FriendshipStatus status = hasReciprocalRequest ?
                User.FriendshipStatus.CONFIRMED : User.FriendshipStatus.UNCONFIRMED;

        user.addFriend(friendId, status);
        userStorage.updateUser(user);

        log.debug("Пользователь id={} добавил в друзья пользователя id={} (статус: {})",
                userId, friendId, status);
    }

    public void removeFriend(Long userId, Long friendId) {
        validateUserId(userId);
        validateUserId(friendId);

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        verificationFriend(userId, friendId);

        if (!user.getFriends().contains(friendId)) {
            log.debug("У пользователя id={} нет друга id={}", userId, friendId);
            return;
        }

        user.removeFriend(friendId);

        // если дружба была подтвержденной, у друга меняем статус на UNCONFIRMED
        if (friend.getFriends().contains(userId)) {
            friend.addFriend(userId, User.FriendshipStatus.UNCONFIRMED);
            userStorage.updateUser(friend);
        }

        userStorage.updateUser(user);

        log.debug("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ConditionsNotMetException("ID пользователя должен быть положительным числом. Получено: " + userId);
        }
    }

    public List<User> getFriends(Long userId) {
        User user = getUserOrThrow(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            User friend = getUserOrThrow(friendId);
            friends.add(friend);
        }
        return friends;
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {

        verificationFriend(userId1, userId2);

        User user1 = getUserOrThrow(userId1);
        User user2 =  getUserOrThrow(userId2);

        Set<Long> friends1 = user1.getFriends();
        Set<Long> friends2 = user2.getFriends();

        List<User> commonFriends = new ArrayList<>();
        for (Long friendId : friends1) {
            if (friends2.contains(friendId)) {
                User friend = getUserOrThrow(friendId);
                commonFriends.add(friend);
            }
        }
        return commonFriends;
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

    private User getUserOrThrow(Long userId) {
        return userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}