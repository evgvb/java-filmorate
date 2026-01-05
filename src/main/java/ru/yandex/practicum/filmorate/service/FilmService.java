package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaService mpaService;
    private final GenreService genreService;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaService mpaService,
                       GenreService genreService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaService = mpaService;
        this.genreService = genreService;
    }

    public void addLike(Long filmId, Long userId) {
        // Проверяем существование фильма и пользователя
        getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        // Проверяем, не поставил ли уже пользователь лайк
        if (filmStorage.hasLike(filmId, userId)) {
            throw new ConditionsNotMetException("Пользователь с id=" + userId +
                    " уже поставил лайк фильму с id=" + filmId);
        }

        // Добавляем лайк через storage
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        // Проверяем существование фильма и пользователя
        getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        // Проверяем, ставил ли пользователь лайк
        if (!filmStorage.hasLike(filmId, userId)) {
            throw new ConditionsNotMetException("Пользователь с id=" + userId +
                    " не ставил лайк фильму с id=" + filmId);
        }

        // Удаляем лайк через storage
        filmStorage.removeLike(filmId, userId);
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
        return getFilmOrThrow(id);
    }

    public Film createFilm(Film film) {
        validateFilm(film);

        // Проверяем существование рейтинга MPA
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaService.validateMpaExists(film.getMpa().getId());
        }

        // Проверяем существование жанра
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            genreService.validateAllGenresExist(film.getGenres());
        }

        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {
        // Проверяем существование фильма
        Film existingFilm = getFilmOrThrow(film.getId());

        validateFilm(film);

        // Проверяем существование рейтинга MPA
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaService.validateMpaExists(film.getMpa().getId());
        }

        // Проверяем существование жанров
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            genreService.validateAllGenresExist(film.getGenres());
        }

        // Сохраняем лайки из существующего фильма
        film.setLikes(existingFilm.getLikes());

        return filmStorage.updateFilm(film);
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ConditionsNotMetException("дата релиза — не раньше 28 декабря 1895 года");
        }
    }

    private Film getFilmOrThrow(Long filmId) {
        return filmStorage.getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
    }

    private User getUserOrThrow(Long userId) {
        return userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

}