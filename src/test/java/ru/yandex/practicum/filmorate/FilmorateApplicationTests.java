package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
// Настройка MockMvc для тестирования веб-слоя
@AutoConfigureMockMvc
class FilmorateApplicationTests {

    //MockMvc для выполнения HTTP запросов
    @Autowired
    private MockMvc mockMvc;

    //ObjectMapper для преобразования объектов в JSON и обратно
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEmptyRequestBody() throws Exception {
        //Выполняем POST запрос с пустым телом
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidJson() throws Exception {
        //проверка обработки не правильного JSON
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFilmWithReleaseDateBeforeMinDate() throws Exception {
        //проверка даты релиза раньше минимальной допустимой (1895-12-28)
        Film film = new Film();
        film.setName("film");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(1890, 1, 1));
        film.setDuration(60);

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"))
                .andExpect(jsonPath("$.message").value("дата релиза — не раньше 28 декабря 1895 года"));
    }


    @Test
    void testFilmWithMinReleaseDate() throws Exception {
        //проверка минимальной допустимой даты релиза
        Film film = new Film();
        film.setName("film");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(60);

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("film"))
                .andExpect(jsonPath("$.releaseDate").value("1895-12-28"));
    }
}