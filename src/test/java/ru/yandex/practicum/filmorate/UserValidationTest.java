package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testCreateValidUser() {
        //проверка правильного создания пользователя
        User user = new User();
        user.setEmail("user@email.ru");
        user.setLogin("user");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty(), "Правильный пользователь должен проходить проверку");
    }


    @Test
    void testEmailIsInvalid() {
        //проверка неверного формата email
        User user = new User();
        user.setEmail("user-email");
        user.setLogin("user");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Неверный формат email не должен проходить проверку");
    }

    @Test
    void testEmailIsNull() {
        //проверка null email
        User user = new User();
        user.setEmail(null);
        user.setLogin("user");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Неверный формат email не должен проходить проверку");
    }

    @Test
    void testLoginIsBlank() {
        //проверка login на пробелы
        User user = new User();
        user.setEmail("user@email.ru");
        user.setLogin(" user  ");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "login пользователя c пробелами не должен проходить проверку");
    }

    @Test
    void testLoginIsNull() {
        //проверка null login
        User user = new User();
        user.setEmail("user@email.ru");
        user.setLogin(null);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "nul login пользователя не должен проходить проверку");
    }


    @Test
    void testWhenBirthdayIsInFuture() {
        //проверка даты рождения в будущем
        User user = new User();
        user.setEmail("user@email.ru");
        user.setLogin("user");
        user.setBirthday(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Пользователь с датой рождения в будущем не должен проходить проверку");
    }

    @Test
    void testBirthdayIsToday() {
        //проверка сегодняшней даты рождения
        User user = new User();
        user.setEmail("user@email.ru");
        user.setLogin("user");
        user.setBirthday(LocalDate.now());

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Пользователь с сегодняшней датой рождения должен проходить проверку");
    }
}