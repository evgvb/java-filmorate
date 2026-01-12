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
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Qualifier("filmDbStorage")
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        // Загружаем MPA
        Integer mpaId = rs.getInt("mpa_id");
        String mpaName = rs.getString("mpa_name");

        if (mpaId != null && !rs.wasNull() && mpaName != null) {
            Mpa mpa = new Mpa(mpaId, mpaName);
            film.setMpa(mpa);
        }

        return film;
    };

    @Override
    public Collection<Film> getAllFilms() {
        String sql = "SELECT f.*, mr.name as mpa_name FROM films f " +
                "LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id " +
                "ORDER BY f.id";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        loadLikesForFilms(films);

        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        try {
            String sql = "SELECT f.*, mr.name as mpa_name FROM films f " +
                    "LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id " +
                    "WHERE f.id = ?";

            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);

            if (film != null) {
                loadLikesForFilm(film);
            }

            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Film createFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().longValue());

        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        return film;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, filmId, userId);
            log.debug("Добавлен лайк: фильм={}, пользователь={}", filmId, userId);
        } catch (Exception e) {
            log.debug("Лайк уже существует: фильм={}, пользователь={}", filmId, userId);
        }
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, filmId, userId);

        if (rowsDeleted > 0) {
            log.debug("Удален лайк: фильм={}, пользователь={}", filmId, userId);
        } else {
            log.debug("Лайк не найден для удаления: фильм={}, пользователь={}", filmId, userId);
        }
    }

    @Override
    public boolean hasLike(Long filmId, Long userId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId, userId);
        return count != null && count > 0;
    }

    @Override
    public void deleteFilm(Long id) {
        // Сначала удаляем зависимости
        String deleteLikesSql = "DELETE FROM likes WHERE film_id = ?";
        jdbcTemplate.update(deleteLikesSql, id);

        String deleteGenresSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteGenresSql, id);

        // Затем удаляем сам фильм
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public boolean containsFilm(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, mr.name as mpa_name, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "GROUP BY f.id, mr.name " +
                "ORDER BY COUNT(l.user_id) DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);

        loadLikesForFilms(films);

        return films;
    }

    private void loadLikesForFilm(Film film) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        List<Long> likes = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong("user_id"),
                film.getId());
        film.setLikes(new HashSet<>(likes));
    }

    private void loadLikesForFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        // Получаем ID фильмов
        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        // Загружаем все лайки для фильмов
        Map<Long, Set<Long>> likesByFilmId = getLikesForFilms(filmIds);

        // Устанавливаем лайки для каждого фильма
        for (Film film : films) {
            Set<Long> likes = likesByFilmId.get(film.getId());
            if (likes != null) {
                film.setLikes(likes);
            } else {
                film.setLikes(new HashSet<>());
            }
        }
    }

    private Map<Long, Set<Long>> getLikesForFilms(List<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = String.format(
                "SELECT film_id, user_id FROM likes WHERE film_id IN (%s)",
                inClause
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, filmIds.toArray());

        Map<Long, Set<Long>> result = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Long filmId = ((Number) row.get("film_id")).longValue();
            Long userId = ((Number) row.get("user_id")).longValue();

            result.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        }

        return result;
    }
}