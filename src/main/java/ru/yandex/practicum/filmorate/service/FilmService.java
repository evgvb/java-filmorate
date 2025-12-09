package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = filmStorage.getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));

        userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        if (film.getLikes().contains(userId)) {
            throw new ConditionsNotMetException("Пользователь с id=" + filmId + " уже поставил лайк фильму с id=" + filmId);
        }

        film.addLike(userId);
        filmStorage.updateFilm(film);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = filmStorage.getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));

        userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        if (!film.getLikes().contains(userId)) {
            throw new ConditionsNotMetException("Пользователь  с id=" + filmId + "не ставил лайк фильму с id=" + filmId);
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
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
    }

    public Film createFilm(Film film) {
        validateFilm(film);
        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {
        return filmStorage.getFilmById(film.getId())
                .map(existingFilm -> {
                    validateFilm(film);

                    if (existingFilm.getLikes() != null) {
                        film.setLikes(existingFilm.getLikes());
                    }
                    return filmStorage.updateFilm(film);
                })
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + film.getId() + " не найден"));
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ConditionsNotMetException("дата релиза — не раньше 28 декабря 1895 года");
        }
    }
}