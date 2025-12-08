package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Collection<Film> getAllFilms();

    Film getFilmById(Long id);

    Film createFilm(Film film);

    Film updateFilm(Film film);

    void deleteFilm(Long id);

    boolean containsFilm(Long id);

    List<Film> getPopularFilms(int count);
}