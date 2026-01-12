package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;
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

    void addFriend(Long userId, Long friendId, User.FriendshipStatus status);

    void removeFriend(Long userId, Long friendId);

    List<Long> getFriends(Long userId);

    boolean hasFriend(Long userId, Long friendId);

    User.FriendshipStatus getFriendshipStatus(Long userId, Long friendId);

    void updateFriendshipStatus(Long userId, Long friendId, User.FriendshipStatus status);

    List<User> getFriendsList(Long userId);

    List<User> getUsersByIds(List<Long> ids);
}
