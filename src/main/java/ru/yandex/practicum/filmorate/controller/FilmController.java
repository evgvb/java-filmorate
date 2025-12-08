package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    public ResponseEntity<Collection<Film>> getFilms() {
        Collection<Film> films = filmService.getAllFilms();
        log.info("Запрошен список фильмов, возвращено {} фильмов", films.size());
        return ResponseEntity.ok(films);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> getFilm(@PathVariable Long id) {
        if (id <= 0) {
            throw new ConditionsNotMetException("ID фильма должен быть положительным числом");
        }

        Film film = filmService.getFilmById(id);
        log.info("Запрошен фильм с id: {}", id);
        return ResponseEntity.ok(film);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Film>> getPopularFilms(
            @RequestParam(defaultValue = "10", required = false) Integer count) {

        if (count <= 0) {
            throw new ConditionsNotMetException("Параметр count должен быть положительным числом");
        }

        List<Film> popularFilms = filmService.getPopularFilms(count);
        log.info("Запрошены {} популярных фильмов", count);
        return ResponseEntity.ok(popularFilms);
    }

    @PostMapping
    public ResponseEntity<Film> createFilm(@Valid @RequestBody Film film) {
        Film createdFilm = filmService.createFilm(film);
        log.info("Создан новый фильм: {}", createdFilm);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFilm);
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@Valid @RequestBody Film film) {
        Film updatedFilm = filmService.updateFilm(film);
        log.info("Фильм обновлен: {}", updatedFilm);
        return ResponseEntity.ok(updatedFilm);
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(
            @PathVariable Long id,
            @PathVariable Long userId) {

        if (id <= 0 || userId <= 0) {
            throw new ConditionsNotMetException("ID фильма и пользователя должны быть положительными числами");
        }

        filmService.addLike(id, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(
            @PathVariable Long id,
            @PathVariable Long userId) {

        if (id <= 0 || userId <= 0) {
            throw new ConditionsNotMetException("ID фильма и пользователя должны быть положительными числами");
        }

        filmService.removeLike(id, userId);
        log.info("Пользователь {} удалил лайк у фильма {}", userId, id);
        return ResponseEntity.ok().build();
    }
}