package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friendships = new HashMap<>();
    private final Map<String, User.FriendshipStatus> friendshipStatuses = new HashMap<>();
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
        friendships.put(user.getId(), new HashSet<>());
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
        friendships.remove(id);

        // Удаляем пользователя в друзьях других пользователей
        for (Set<Long> friends : friendships.values()) {
            friends.remove(id);
        }
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

    @Override
    public void addFriend(Long userId, Long friendId, User.FriendshipStatus status) {
        friendships.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        friendshipStatuses.put(getFriendshipKey(userId, friendId), status);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        Set<Long> userFriends = friendships.get(userId);
        if (userFriends != null) {
            userFriends.remove(friendId);
        }
        friendshipStatuses.remove(getFriendshipKey(userId, friendId));
    }

    @Override
    public List<Long> getFriends(Long userId) {
        Set<Long> friends = friendships.get(userId);
        return friends != null ? new ArrayList<>(friends) : new ArrayList<>();
    }

    @Override
    public boolean hasFriend(Long userId, Long friendId) {
        Set<Long> userFriends = friendships.get(userId);
        return userFriends != null && userFriends.contains(friendId);
    }

    @Override
    public User.FriendshipStatus getFriendshipStatus(Long userId, Long friendId) {
        return friendshipStatuses.getOrDefault(
                getFriendshipKey(userId, friendId),
                User.FriendshipStatus.UNCONFIRMED
        );
    }

    @Override
    public void updateFriendshipStatus(Long userId, Long friendId, User.FriendshipStatus status) {
        friendshipStatuses.put(getFriendshipKey(userId, friendId), status);
    }

    private String getFriendshipKey(Long userId, Long friendId) {
        return userId + "_" + friendId;
    }

    @Override
    public List<User> getFriendsList(Long userId) {
        List<Long> friendIds = getFriends(userId);
        return getUsersByIds(friendIds);
    }

    @Override
    public List<User> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        return ids.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}