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
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@Repository
@Qualifier("filmDbStorage")
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private JdbcTemplate jdbcTemplate;
    private GenreService genreService;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate, GenreService genreService) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreService = genreService;
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

    private void loadLikes(Film film) {
        String sqlLikes = "SELECT user_id FROM likes WHERE film_id = ?";
        List<Long> likes = jdbcTemplate.query(sqlLikes, (rsLikes, rowNumLikes) ->
                rsLikes.getLong("user_id"), film.getId());
        film.setLikes(new HashSet<>(likes));
    }

    @Override
    public Collection<Film> getAllFilms() {
        String sql = "SELECT f.*, mr.name as mpa_name FROM films f " +
                "LEFT JOIN mpa_ratings mr ON f.mpa_id = mr.id " +
                "ORDER BY f.id";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        if (!films.isEmpty()) {
            genreService.addGenresFilms(films);
            films.forEach(this::loadLikes);
        }
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
                genreService.addGenresFilm(film);
                loadLikes(film);
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

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            genreService.setFilmGenres(film.getId(), new ArrayList<>(film.getGenres()));
        }

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

        genreService.deleteFilmGenres(film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            genreService.setFilmGenres(film.getId(), new ArrayList<>(film.getGenres()));
        }

        return film;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, filmId, userId);
            log.debug("Добавлен лайк: фильм={}, пользователь={}", filmId, userId);
        } catch (Exception e) {
            // Если лайк уже существует, это нормально
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
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);

        if (!films.isEmpty()) {
            genreService.addGenresFilms(films);
            films.forEach(this::loadLikes);
        }

        return films;
    }
}