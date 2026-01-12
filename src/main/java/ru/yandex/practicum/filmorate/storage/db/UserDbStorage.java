package ru.yandex.practicum.filmorate.storage.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@Repository
@Qualifier("userDbStorage")
@Slf4j
public class UserDbStorage implements UserStorage {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        //user.setId(rs.getLong("id"));
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());

        return user;
    };

    @Override
    public Collection<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY id";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public Optional<User> getUserById(Long id) {

        try {
            String sql = "SELECT * FROM users WHERE id = ?";
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public User createUser(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        user.setId(keyHolder.getKey().longValue());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());

        return user;
    }

    @Override
    public void deleteUser(Long id) {

        // удаляем дружбы пользователя
        String deleteFriendshipsSql = "DELETE FROM friendships WHERE user_id = ? OR friend_id = ?";
        jdbcTemplate.update(deleteFriendshipsSql, id, id);

        // Удаляем лайки пользователя
        String deleteLikesSql = "DELETE FROM likes WHERE user_id = ?";
        jdbcTemplate.update(deleteLikesSql, id);

        // Удаляем пользователя
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public boolean containsUser(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public boolean isLoginExists(String login) {
        String sql = "SELECT COUNT(*) FROM users WHERE login = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, login);
        return count != null && count > 0;
    }

    @Override
    public boolean isEmailExists(String email, Long excludeUserId) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND id != ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, excludeUserId);
        return count != null && count > 0;
    }

    @Override
    public boolean isLoginExists(String login, Long excludeUserId) {
        String sql = "SELECT COUNT(*) FROM users WHERE login = ? AND id != ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, login, excludeUserId);
        return count != null && count > 0;
    }

    @Override
    public void addFriend(Long userId, Long friendId, User.FriendshipStatus status) {
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)";
        try {
            jdbcTemplate.update(sql, userId, friendId, status.toString());
            log.debug("Добавлен друг: пользователь={}, друг={}, статус={}", userId, friendId, status);
        } catch (Exception e) {
            // Если дружба уже существует, обновляем статус
            log.debug("Дружба уже существует, обновляем статус: пользователь={}, друг={}", userId, friendId);
            updateFriendshipStatus(userId, friendId, status);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, userId, friendId);

        if (rowsDeleted > 0) {
            log.debug("Удален друг: пользователь={}, друг={}", userId, friendId);
            if (hasFriend(friendId, userId)) {
                updateFriendshipStatus(friendId, userId, User.FriendshipStatus.UNCONFIRMED);
            } else {
                log.debug("Друг не найден для удаления: пользователь={}, друг={}", userId, friendId);
            }
        }
    }

    @Override
    public List<Long> getFriends(Long userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ? ORDER BY friend_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("friend_id"), userId);
    }

    @Override
    public boolean hasFriend(Long userId, Long friendId) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId);
        return count != null && count > 0;
    }

    @Override
    public User.FriendshipStatus getFriendshipStatus(Long userId, Long friendId) {
        String sql = "SELECT status FROM friendships WHERE user_id = ? AND friend_id = ?";
        try {
            String statusStr = jdbcTemplate.queryForObject(sql, String.class, userId, friendId);
            return User.FriendshipStatus.valueOf(statusStr);
        } catch (EmptyResultDataAccessException e) {
            return User.FriendshipStatus.UNCONFIRMED;
        }
    }

    @Override
    public void updateFriendshipStatus(Long userId, Long friendId, User.FriendshipStatus status) {
        String sql = "UPDATE friendships SET status = ? WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, status.toString(), userId, friendId);
    }

    @Override
    public List<User> getFriendsList(Long userId) {
        String sql = "SELECT u.* FROM users u JOIN friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ? ORDER BY u.id";
        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    @Override
    public List<User> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        String inClause = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = String.format("SELECT * FROM users WHERE id IN (%s) ORDER BY id", inClause);

        return jdbcTemplate.query(sql, userRowMapper, ids.toArray());
    }
}