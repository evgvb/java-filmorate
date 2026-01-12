package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GenreStorage {
    List<Genre> getAllGenres();

    Optional<Genre> getGenreById(Integer id);

    List<Genre> getFilmGenres(Long filmId);

    Map<Long, List<Genre>> getGenresForFilms(List<Long> filmIds);

    void setFilmGenres(Long filmId, List<Genre> genres);

    void deleteFilmGenres(Long filmId);
}