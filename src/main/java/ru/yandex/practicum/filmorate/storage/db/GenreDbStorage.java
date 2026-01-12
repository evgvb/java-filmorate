package ru.yandex.practicum.filmorate.storage.db;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.util.*;

@Repository
@Qualifier("genreDbStorage")
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;

    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Genre> genreRowMapper = (rs, rowNum) ->
            new Genre(rs.getInt("id"), rs.getString("name"));

    @Override
    public List<Genre> getAllGenres() {
        String sql = "SELECT * FROM genres ORDER BY id";
        return jdbcTemplate.query(sql, genreRowMapper);
    }

    @Override
    public Optional<Genre> getGenreById(Integer id) {
        try {
            String sql = "SELECT * FROM genres WHERE id = ?";
            Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper, id);

            return Optional.ofNullable(genre);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Genre> getFilmGenres(Long filmId) {
        String sql = "SELECT g.* FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.id";
        return jdbcTemplate.query(sql, genreRowMapper, filmId);
    }

    @Override
    public void setFilmGenres(Long filmId, List<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.getId());
        }
    }

    @Override
    public void deleteFilmGenres(Long filmId) {
        String sql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }

    @Override
    public Map<Long, List<Genre>> getGenresForFilms(List<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return new HashMap<>();
        }

        // Collections.nCopies(n, element) - создает список из n копий элемента "?"
        // String.join объединяет их запятыми: для 3 filmIds -> "?,?,?"
        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));

        // Формируем SQL-запрос с использованием форматирования строк
        String sql = String.format(
                "SELECT fg.film_id, g.* " +
                        "FROM film_genres fg " +
                        "JOIN genres g ON fg.genre_id = g.id " +
                        "WHERE fg.film_id IN (%s) " +
                        "ORDER BY fg.film_id, g.id",
                inClause    // Подставляем в IN-условие:
        );

        // filmIds.toArray() передает параметры для подстановки вместо "?"
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, filmIds.toArray());

        Map<Long, List<Genre>> result = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Long filmId = ((Number) row.get("film_id")).longValue();
            Genre genre = new Genre(
                    ((Number) row.get("id")).intValue(),
                    (String) row.get("name")
            );

            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        }

        // Для фильмов без жанров добавляем пустые списки
        for (Long filmId : filmIds) {
            if (!result.containsKey(filmId)) {
                result.put(filmId, new ArrayList<>());
            }
        }

        return result;
    }
}