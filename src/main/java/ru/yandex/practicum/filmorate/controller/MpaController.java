package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@Slf4j
public class MpaController {

    private final MpaStorage mpaStorage;

    @Autowired
    public MpaController(MpaStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    @GetMapping
    public ResponseEntity<List<Mpa>> getAllMpa() {
        List<Mpa> mpaList = mpaStorage.getAllMpa();
        log.info("Запрошен список всех рейтингов MPA, найдено {} рейтингов", mpaList.size());
        return ResponseEntity.ok(mpaList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mpa> getMpaById(@PathVariable Integer id) {
        if (id < 1) {
            throw new NotFoundException("ID рейтинга MPA должен быть положительным числом");
        }

        Mpa mpa = mpaStorage.getMpaById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id=" + id + " не найден"));

        log.info("Запрошен рейтинг MPA с id: {}", id);
        return ResponseEntity.ok(mpa);
    }
}