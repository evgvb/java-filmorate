package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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

    private Set<Long> likes = new HashSet<>();

    public void addLike(Long userId) {
        likes.add(userId);
    }

    public void removeLike(Long userId) {
        likes.remove(userId);
    }

    public Set<Long> getLikes() {
        return new HashSet<>(likes);
    }

    public int getLikesCount() {
        return likes.size();
    }
}
