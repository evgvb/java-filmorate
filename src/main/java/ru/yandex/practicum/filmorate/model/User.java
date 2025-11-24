package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class User {
    Long id;

    @NotBlank(message = "электронная почта не может быть пустой")
    @Pattern(regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$",
            message = "не корректный email") //электронная почта должна содержать символ @
    String email;

    @NotBlank(message = "логин не может быть пустым")
    @Pattern(regexp = "\\S+", message = "логин не может содержать пробелы")
    String login;

    String name;

    @PastOrPresent(message = "дата рождения не может быть в будущем")
    LocalDate birthday;

    public String getName() {
        if (name == null || name.isBlank()) {
            return login;
        }
        return name;
    }
}
