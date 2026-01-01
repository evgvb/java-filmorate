-- Очистка данных (опционально, так как БД создается заново)
DELETE
FROM likes;
DELETE
FROM film_genres;
DELETE
FROM friendships;
DELETE
FROM films;
DELETE
FROM users;

-- Заполнение пользователей
INSERT INTO users (id, email, login, name, birthday)
VALUES (1, 'user1@email.com', 'user1', 'User 1', '1990-01-01'),
       (2, 'user2@email.com', 'user2', 'User 2', '1991-02-02'),
       (3, 'user3@email.com', 'user3', 'User 3', '1992-03-03');

-- Заполнение фильмов
INSERT INTO films (id, name, description, release_date, duration, mpa_id)
VALUES (1, 'Film1', 'Description 1', '2000-01-01', 120, 1),
       (2, 'Film2', 'Description 2', '2001-02-02', 90, 2),
       (3, 'Film3', 'Description 3', '2002-03-03', 150, 3);

-- Заполнение жанров фильмов
INSERT INTO film_genres (film_id, genre_id)
VALUES (1, 1),
       (1, 2), -- Film One: Комедия, Драма
       (2, 3),
       (2, 4), -- Film Two: Мультфильм, Триллер
       (3, 5);
-- Film Three: Документальный

-- Заполнение лайков
INSERT INTO likes (film_id, user_id)
VALUES (1, 1),
       (1, 2), -- Film One liked by users 1 and 2
       (2, 1);
-- Film Two liked by user 1

-- Заполнение дружбы
INSERT INTO friendships (user_id, friend_id, status)
VALUES (1, 2, 'CONFIRMED'),
       (1, 3, 'UNCONFIRMED'),
       (2, 3, 'CONFIRMED');

-- Сброс sequence для автоинкремента
ALTER TABLE users
    ALTER COLUMN id RESTART WITH 4;
ALTER TABLE films
    ALTER COLUMN id RESTART WITH 4;