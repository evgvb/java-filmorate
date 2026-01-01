package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.db.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
//@Slf4j
class FilmorateApplicationTests {

    private final UserDbStorage userDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final MpaDbStorage mpaDbStorage;
    private final GenreDbStorage genreDbStorage;

    @Test
    void contextLoads() {
        // проверяем, что контекст Spring загружается
        assertThat(userDbStorage).isNotNull();
        assertThat(filmDbStorage).isNotNull();
        assertThat(mpaDbStorage).isNotNull();
        assertThat(genreDbStorage).isNotNull();
    }

    @Test
    void testCreateUser() {
        // проверяем, создание пользователя
        User testUser1 = new User();
        testUser1.setEmail("userTest@email.com");
        testUser1.setLogin("userTest");
        testUser1.setName("User Test");
        testUser1.setBirthday(LocalDate.of(1990, 1, 1));
        User createdUser = userDbStorage.createUser(testUser1);

        // проверяем, что ID установлен
        assertThat(createdUser.getId()).isNotNull();

        Optional<User> foundUser = userDbStorage.getUserById(createdUser.getId());

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getId()).isEqualTo(createdUser.getId());
                    assertThat(user.getEmail()).isEqualTo("userTest@email.com");
                    assertThat(user.getLogin()).isEqualTo("userTest");
                    assertThat(user.getName()).isEqualTo("User Test");
                    assertThat(user.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
                });
    }

    @Test
    void testUpdateUser() {
        // проверяем обновление пользователя user1 (test/resources/data.sql)
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("updated@email.com");
        updatedUser.setLogin("updated");
        updatedUser.setName("Updated Name");
        updatedUser.setBirthday(LocalDate.of(1991, 2, 2));

        userDbStorage.updateUser(updatedUser);

        Optional<User> foundUser = userDbStorage.getUserById(1L);

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getEmail()).isEqualTo("updated@email.com");
                    assertThat(user.getLogin()).isEqualTo("updated");
                    assertThat(user.getName()).isEqualTo("Updated Name");
                    assertThat(user.getBirthday()).isEqualTo(LocalDate.of(1991, 2, 2));
                });
    }

    @Test
    void testDeleteUser() {
        // удаляем пользователя. id = 1
        userDbStorage.deleteUser(1L);

        // Проверяем, что пользователь удален
        assertThat(userDbStorage.containsUser(1L)).isFalse();
        assertThat(userDbStorage.getUserById(1L)).isEmpty();
    }

    @Test
    public void testFindUserById() {
        // проверяем поиск пользователя. id = 2
        Optional<User> userOptional = userDbStorage.getUserById(2L);

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user).hasFieldOrPropertyWithValue("id", 2L)
                );
    }

    @Test
    void testUserFriends() {
        // Создаем пользователей
        User testUser4 = new User();
        testUser4.setEmail("user4@email.com");
        testUser4.setLogin("user4");
        testUser4.setName("User4");
        testUser4.setBirthday(LocalDate.of(1990, 1, 1));

        User testUser5 = new User();
        testUser5.setEmail("user5@email.com");
        testUser5.setLogin("user5");
        testUser5.setName("User 5");
        testUser5.setBirthday(LocalDate.of(1995, 5, 5));

        User user4 = userDbStorage.createUser(testUser4);
        User user5 = userDbStorage.createUser(testUser5);

        // Добавляем друзей
        user4.addFriend(user5.getId(), User.FriendshipStatus.CONFIRMED);
        user5.addFriend(user4.getId(), User.FriendshipStatus.CONFIRMED);

        // Обновляем пользователей
        userDbStorage.updateUser(user4);
        userDbStorage.updateUser(user5);

        // Проверяем друзей у user4
        Optional<User> foundUser4 = userDbStorage.getUserById(user4.getId());
        assertThat(foundUser4)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getFriends()).contains(user5.getId());
                    assertThat(user.getFriendshipStatuses().get(user5.getId()))
                            .isEqualTo(User.FriendshipStatus.CONFIRMED);
                });

        // Проверяем друзей у user5
        Optional<User> foundUser5 = userDbStorage.getUserById(user5.getId());
        assertThat(foundUser5)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getFriends()).contains(user4.getId());
                });
    }

    @Test
    public void testFindFilmById() {
        Optional<Film> filmOptional = filmDbStorage.getFilmById(1L);

        assertThat(filmOptional)
                .isPresent()
                .hasValueSatisfying(film ->
                        assertThat(film).hasFieldOrPropertyWithValue("id", 1L)
                );
    }

    @Test
    void testUpdateFilm() {
        // Обновляем фильм. id =  1
        Film updatedFilm = new Film();
        updatedFilm.setId(1L);
        updatedFilm.setName("Updated Film");
        updatedFilm.setDescription("Updated description");
        updatedFilm.setReleaseDate(LocalDate.of(2001, 1, 1));
        updatedFilm.setDuration(130);
        updatedFilm.setMpa(new Mpa(2)); // PG

        Set<Genre> updatedGenres = new HashSet<>();
        updatedGenres.add(new Genre(3)); // Мультфильм
        updatedFilm.setGenres(updatedGenres);

        filmDbStorage.updateFilm(updatedFilm);

        // Проверяем обновление
        Optional<Film> foundFilm = filmDbStorage.getFilmById(1L);

        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(film -> {
                    assertThat(film.getName()).isEqualTo("Updated Film");
                    assertThat(film.getDescription()).isEqualTo("Updated description");
                    assertThat(film.getReleaseDate()).isEqualTo(LocalDate.of(2001, 1, 1));
                    assertThat(film.getDuration()).isEqualTo(130);
                    assertThat(film.getMpa()).isNotNull();
                    assertThat(film.getMpa().getId()).isEqualTo(2);
                    assertThat(film.getGenres()).hasSize(1);
                    assertThat(film.getGenres())
                            .extracting(Genre::getId)
                            .containsExactly(3);
                });
    }

    @Test
    void testDeleteFilm() {
        // Создаем фильм
        Film testFilm1 = new Film();
        testFilm1.setName("Film One");
        testFilm1.setDescription("Description of film one");
        testFilm1.setReleaseDate(LocalDate.of(2000, 1, 1));
        testFilm1.setDuration(120);
        testFilm1.setMpa(new Mpa(1));

        Film createdFilm = filmDbStorage.createFilm(testFilm1);
        Long filmId = createdFilm.getId();

        // Проверяем, что фильм существует
        assertThat(filmDbStorage.containsFilm(filmId)).isTrue();

        // Удаляем фильм
        filmDbStorage.deleteFilm(filmId);

        // Проверяем, что фильм удален
        assertThat(filmDbStorage.containsFilm(filmId)).isFalse();
        assertThat(filmDbStorage.getFilmById(filmId)).isEmpty();
    }

    @Test
    void testFilmLikes() {
        // Проверяем постановку лайков
        Optional<User> userOptional = userDbStorage.getUserById(2L);

        assertThat(userOptional)
                .as("Пользователь с ID=2 должен существовать")
                .isPresent();
        User user = userOptional.get();

        Optional<Film> filmOptional = filmDbStorage.getFilmById(3L);

        assertThat(filmOptional)
                .as("Фильм с ID=2 должен существовать")
                .isPresent();

        Film film = filmOptional.get();

        film.addLike(user.getId());

        Film updatedFilm = filmDbStorage.updateFilm(film);

        Optional<Film> foundFilm = filmDbStorage.getFilmById(film.getId());

        assertThat(foundFilm)
                .as("Фильм должен быть найден в БД после обновления")
                .isPresent()
                .hasValueSatisfying(filmWithLike -> {
                    assertThat(filmWithLike.getLikes())
                            .as("Список лайков должен содержать ID пользователя")
                            .contains(user.getId());

                    int expectedLikesCount = 1;

                    assertThat(filmWithLike.getLikesCount())
                            .as("Количество лайков должно быть равно 2")
                            .isEqualTo(expectedLikesCount);
                });
    }

    @Test
    void testGetRealMpaById() {
        // Проверяем существующий рейтинг
        Optional<Mpa> mpaOptional = mpaDbStorage.getMpaById(1);

        assertThat(mpaOptional)
                .isPresent()
                .hasValueSatisfying(mpa -> {
                    assertThat(mpa.getId()).isEqualTo(1);
                    assertThat(mpa.getName()).isEqualTo("G");
                });
    }

    @Test
    void testGetUnrealMpaById() {
        // Проверяем несуществующий рейтинг
        Optional<Mpa> nonExistentMpa = mpaDbStorage.getMpaById(999);
        assertThat(nonExistentMpa).isEmpty();
    }

    @Test
    void testGetRealGenreById() {
        // Проверяем существующий жанр
        Optional<Genre> genreOptional = genreDbStorage.getGenreById(1);

        assertThat(genreOptional)
                .isPresent()
                .hasValueSatisfying(genre -> {
                    assertThat(genre.getId()).isEqualTo(1);
                    assertThat(genre.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    void testGetUnrealGenreById() {
        // Проверяем несуществующий жанр
        Optional<Genre> nonExistentGenre = genreDbStorage.getGenreById(999);
        assertThat(nonExistentGenre).isEmpty();
    }

    @Test
    @Order(18)
    void testGetFilmGenres() {
        // Создаем фильм с жанрами
        Film testFilm1 = new Film();
        testFilm1.setName("Film 4");
        testFilm1.setDescription("Description 4");
        testFilm1.setReleaseDate(LocalDate.of(2000, 1, 1));
        testFilm1.setDuration(120);
        testFilm1.setMpa(new Mpa(1));

        Set<Genre> genres1 = new HashSet<>();
        genres1.add(new Genre(1));
        genres1.add(new Genre(2));
        testFilm1.setGenres(genres1);

        Film film = filmDbStorage.createFilm(testFilm1);

        // Получаем жанры фильма
        List<Genre> filmGenres = genreDbStorage.getFilmGenres(film.getId());

        assertThat(filmGenres)
                .hasSize(2)
                .extracting(Genre::getId)
                .containsExactlyInAnyOrder(1, 2);
    }
}

