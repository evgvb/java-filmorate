package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaStorage mpaStorage,
                       GenreStorage genreStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        if (film.getLikes().contains(userId)) {
            throw new ConditionsNotMetException("Пользователь с id=" + filmId + " уже поставил лайк фильму с id=" + filmId);
        }

        film.addLike(userId);
        filmStorage.updateFilm(film);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        if (!film.getLikes().contains(userId)) {
            throw new ConditionsNotMetException("Пользователь  с id=" + userId + "не ставил лайк фильму с id=" + filmId);
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
        return getFilmOrThrow(id);
    }

    public Film createFilm(Film film) {
        validateFilm(film);

        // Проверяем существование рейтинга MPA
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            validateMpaExists(film.getMpa().getId());
        }

        // Проверяем существование жанра
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            validateAllGenresExist(film.getGenres());
        }

        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {
        return filmStorage.getFilmById(film.getId())
                .map(existingFilm -> {
                    validateFilm(film);

                    if (existingFilm.getLikes() != null) {
                        film.setLikes(existingFilm.getLikes());
                    }

                    if (film.getMpa() != null && film.getMpa().getId() != null) {
                        validateMpaExists(film.getMpa().getId());
                    }

                    if (film.getGenres() != null && !film.getGenres().isEmpty()) {
                        validateAllGenresExist(film.getGenres());
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

    private Film getFilmOrThrow(Long filmId) {
        return filmStorage.getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + filmId + " не найден"));
    }

    private User getUserOrThrow(Long userId) {
        return userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private void validateMpaExists(Integer mpaId) {
        if (mpaId != null) {
            mpaStorage.getMpaById(mpaId)
                    .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id=" + mpaId + " не найден"));
        }
    }

    private void validateAllGenresExist(Set<Genre> genres) {
        Set<Integer> invalidGenreIds = new HashSet<>();

        for (Genre genre : genres) {
            Optional<Genre> existingGenre = genreStorage.getGenreById(genre.getId());
            if (existingGenre.isEmpty()) {
            //if (existingGenre == null) {
                invalidGenreIds.add(genre.getId());
            }
        }

        if (!invalidGenreIds.isEmpty()) {
            throw new NotFoundException("Жанры с id=" + invalidGenreIds + " не найдены");
        }
    }
}