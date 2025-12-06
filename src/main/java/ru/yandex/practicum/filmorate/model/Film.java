package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Film {
    Long id;

    @NotBlank(message = "название не может быть пустым")
    String name;

    @Size(max = 200, message = "максимальная длина описания — 200 символов")
    String description;

    //  дата релиза — не раньше 28 декабря 1895 года;
    @NotNull
    LocalDate releaseDate;

    @NotNull
    @Positive(message = "продолжительность фильма должна быть положительным числом")
    Integer duration;
}
