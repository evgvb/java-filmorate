package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GenreService {

    private final GenreStorage genreStorage;

    @Autowired
    public GenreService(GenreStorage genreStorage) {
        this.genreStorage = genreStorage;
    }

    public List<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }

    public Genre getGenreById(Integer id) {
        if (id == null || id < 1) {
            throw new NotFoundException("ID жанра должен быть положительным числом");
        }

        return genreStorage.getGenreById(id)
                .orElseThrow(() -> new NotFoundException("Жанр с id=" + id + " не найден"));
    }

    public List<Genre> getFilmGenres(Long filmId) {
        return genreStorage.getFilmGenres(filmId);
    }

    public void setFilmGenres(Long filmId, List<Genre> genres) {
        genreStorage.setFilmGenres(filmId, genres);
    }

    public void deleteFilmGenres(Long filmId) {
        genreStorage.deleteFilmGenres(filmId);
    }

    public void validateAllGenresExist(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        for (Genre genre : genres) {
            getGenreById(genre.getId());
        }
    }

    public Map<Long, List<Genre>> getGenresForFilms(List<Long> filmIds) {
        return genreStorage.getGenresForFilms(filmIds);
    }

    public void addGenresFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        // получаем фильмы
        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        // получаем жанры
        Map<Long, List<Genre>> genresByFilmId = getGenresForFilms(filmIds);

        // добавляем жанры фильмов
        for (Film film : films) {
            List<Genre> genres = genresByFilmId.get(film.getId());
            if (genres != null && !genres.isEmpty()) {
                film.setGenres(new HashSet<>(genres));
            }
        }
    }

    public void addGenresFilm(Film film) {
        if (film == null || film.getId() == null) {
            return;
        }

        List<Genre> genres = getFilmGenres(film.getId());
        if (!genres.isEmpty()) {
            film.setGenres(new HashSet<>(genres));
        }
    }
}