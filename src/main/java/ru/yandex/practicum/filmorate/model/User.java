package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {
    private Long id;

    @NotBlank(message = "электронная почта не может быть пустой")
    @Pattern(regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$",
            message = "не корректный email")
    private String email;

    @NotBlank(message = "логин не может быть пустым")
    @Pattern(regexp = "\\S+", message = "логин не может содержать пробелы")
    private String login;

    private String name;

    @NotNull
    @PastOrPresent(message = "дата рождения не может быть в будущем")
    private LocalDate birthday;

    private Set<Long> friends = new HashSet<>();

    public String getName() {
        if (name == null || name.isBlank()) {
            return login;
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addFriend(Long friendId) {
        friends.add(friendId);
    }

    public void removeFriend(Long friendId) {
        friends.remove(friendId);
    }

    public Set<Long> getFriends() {
        return new HashSet<>(friends);
    }

    public void setFriends(Set<Long> friends) {
        this.friends = friends != null ? new HashSet<>(friends) : new HashSet<>();
    }
}