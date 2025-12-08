package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = filmStorage.getFilmById(filmId);
        userStorage.getUserById(userId);

        if (film.getLikes().contains(userId)) {
            throw new ConditionsNotMetException("Пользователь уже поставил лайк этому фильму");
        }

        film.addLike(userId);
        filmStorage.updateFilm(film);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = filmStorage.getFilmById(filmId);
        userStorage.getUserById(userId);

        if (!film.getLikes().contains(userId)) {
            throw new ConditionsNotMetException("Пользователь не ставил лайк этому фильму");
        }

        film.removeLike(userId);
        filmStorage.updateFilm(film);
    }

    public List<Film> getPopularFilms(int count) {
        if (count <= 0) {
            throw new ConditionsNotMetException("Параметр count должен быть положительным числом");
        }

        return filmStorage.getPopularFilms(count);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film getFilmById(Long id) {
        return filmStorage.getFilmById(id);
    }

    public Film createFilm(Film film) {
        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {
        if (!filmStorage.containsFilm(film.getId())) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        return filmStorage.updateFilm(film);
    }

    public boolean containsFilm(Long id) {
        return filmStorage.containsFilm(id);
    }
}