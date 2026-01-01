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

        // Загружаем друзей
        Set<Long> friends = new HashSet<>();
        Map<Long, User.FriendshipStatus> statuses = new HashMap<>();

        String sqlFriends = "SELECT friend_id, status FROM friendships WHERE user_id = ?";
        jdbcTemplate.query(sqlFriends, (rsFriends) -> {
            Long friendId = rsFriends.getLong("friend_id");
            String status = rsFriends.getString("status");
            friends.add(friendId);
            statuses.put(friendId, User.FriendshipStatus.valueOf(status));
        }, user.getId());

        user.setFriends(friends);
        // Здесь должен быть сеттер для friendshipStatuses
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

            if (user != null) {
                loadFriends(user);
            }

            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private void loadFriends(User user) {
        String sql = "SELECT friend_id, status FROM friendships WHERE user_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, user.getId());

        for (Map<String, Object> row : rows) {
            Long friendId = (Long) row.get("friend_id");
            String statusStr = (String) row.get("status");
            User.FriendshipStatus status = User.FriendshipStatus.valueOf(statusStr);

            user.addFriend(friendId, status);
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

        //user.setId(keyHolder.getKey().longValue());
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

        // Обновляем друзей
        updateFriends(user);

        return user;
    }

    private void updateFriends(User user) {
        // Удаляем старые записи
        String deleteSql = "DELETE FROM friendships WHERE user_id = ?";
        jdbcTemplate.update(deleteSql, user.getId());

        // Добавляем заново
        if (user.getFriends() != null && !user.getFriends().isEmpty()) {
            String insertSql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)";
            for (Long friendId : user.getFriends()) {
                User.FriendshipStatus status = user.getFriendshipStatuses().get(friendId);
                jdbcTemplate.update(insertSql,
                        user.getId(),
                        friendId,
                        status != null ? status.toString() : User.FriendshipStatus.UNCONFIRMED.toString());
            }
        }

    }

    @Override
    public void deleteUser(Long id) {
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
}