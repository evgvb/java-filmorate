package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FilmValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testCreateValidFilm() {
        //проверка правильного создания фильма
        Film film = new Film();
        film.setName("film");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Правильный фильм должен проходить проверку");
    }

    @Test
    void testNameIsBlank() {
        //проверка пустого названия, только пробелы
        Film film = new Film();
        film.setName("   ");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Фильм с пустым названием не должен проходить проверку");
    }

    @Test
    void testNameIsNull() {
        //проверка null названия
        Film film = new Film();
        // Устанавливаем название как null
        film.setName(null);
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Фильм с null названием не должен проходить проверку");
    }

    @Test
    void testDescriptionIsTooLong() {
        //проверка превышения максимальной длины описания
        Film film = new Film();
        film.setName("film");
        film.setDescription("A".repeat(201));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Фильм с описанием длиннее 200 символов не должен проходить проверку");
    }

    @Test
    void testDescriptionIsExactly200Characters() {
        //проверка максимальной длины описания (ровно 200 символов)
        Film film = new Film();
        film.setName("film");
        film.setDescription("A".repeat(200));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Фильм с описанием ровно 200 символов должен проходить проверку");
    }

    @Test
    void testDurationIsNegative() {
        //проверка отрицательной продолжительности
        Film film = new Film();
        film.setName("film");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(-10);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Фильм с отрицательной продолжительностью не должен проходить проверку");
    }

    @Test
    void testDurationIsZero() {
        //проверка нулевой продолжительности
        Film film = new Film();
        film.setName("film");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Фильм с нулевой продолжительностью не должен проходить проверку");
    }
}