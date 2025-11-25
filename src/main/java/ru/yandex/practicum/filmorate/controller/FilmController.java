package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();

    // день рождения кино
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Попытка создать фильм с датой релиза раньше 28.12.1895: {}", film.getReleaseDate());
            throw new ConditionsNotMetException("дата релиза — не раньше 28 декабря 1895 года");
        }
    }

    @GetMapping
    public ResponseEntity<Collection<Film>> getFilms() {
        log.info("Запрошен список фильмов, возвращено {} фильмов", films.size());
        return ResponseEntity.ok(films.values());
    }

    @PostMapping
    public ResponseEntity<Film> createFilm(@Valid @RequestBody Film film) {
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Создан новый фильм: {}", film);
        return ResponseEntity.status(HttpStatus.CREATED).body(film);
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@RequestBody Film film) {
        if (film.getId() == null && !films.containsKey(film.getId())) {
            log.warn("Фильм не найден. id: {}", film.getId());
            throw new NotFoundException("Фильм не найден id: " + film.getId());
        }

        Film updatedFilm = films.get(film.getId());
        if (film.getName() != null || !film.getName().isBlank()) {
            updatedFilm.setName(film.getName());
        }
        if (film.getDescription() != null) {
            updatedFilm.setDescription(film.getDescription());
        }
        if (film.getReleaseDate() != null) {
            validateFilm(film);
            updatedFilm.setReleaseDate(film.getReleaseDate());
        }
        if (film.getDuration() != null) {
            updatedFilm.setDuration(film.getDuration());
        }
        log.info("Фильм обновлен: {}", updatedFilm);
        return ResponseEntity.ok(updatedFilm);
    }
}