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

        // Добавляем лайк
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

        // Удаляем лайк
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        if (count <= 0) {
            throw new ConditionsNotMetException("Параметр count должен быть положительным числом");
        }

        List<Film> films = filmStorage.getPopularFilms(count);
        genreService.addGenresFilms(films);
        return films;
    }

    public Collection<Film> getAllFilms() {
        Collection<Film> films = filmStorage.getAllFilms();
        if (films instanceof List) {
            genreService.addGenresFilms((List<Film>) films);
        } else {
            List<Film> filmList = new ArrayList<>(films);
            genreService.addGenresFilms(filmList);
        }
        return films;
    }

    public Film getFilmById(Long id) {
        Film film = getFilmOrThrow(id);
        genreService.addGenresFilm(film);
        return film;
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

            Set<Genre> uniqueGenres = new LinkedHashSet<>(film.getGenres());
            film.setGenres(uniqueGenres);
        }

        Film createdFilm = filmStorage.createFilm(film);

        if (createdFilm.getGenres() != null && !createdFilm.getGenres().isEmpty()) {
            genreService.setFilmGenres(createdFilm.getId(), new ArrayList<>(createdFilm.getGenres()));
        }

        return createdFilm;
    }

    public Film updateFilm(Film film) {
        // Проверяем существование фильма
        Film existingFilm = getFilmOrThrow(film.getId());

        validateFilm(film);

        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaService.validateMpaExists(film.getMpa().getId());
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            genreService.validateAllGenresExist(film.getGenres());

            Set<Genre> uniqueGenres = new LinkedHashSet<>(film.getGenres());
            film.setGenres(uniqueGenres);
        }

        film.setLikes(existingFilm.getLikes());

        Film updatedFilm = filmStorage.updateFilm(film);

        genreService.deleteFilmGenres(updatedFilm.getId());
        if (updatedFilm.getGenres() != null && !updatedFilm.getGenres().isEmpty()) {
            genreService.setFilmGenres(updatedFilm.getId(), new ArrayList<>(updatedFilm.getGenres()));
        }

        return updatedFilm;
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