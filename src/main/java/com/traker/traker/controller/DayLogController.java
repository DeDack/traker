package com.traker.traker.controller;

import com.traker.traker.dto.entity.TimeEntryDto;
import com.traker.traker.service.DayLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления записями времени и днями.
 */
@RestController
@RequestMapping("/api/days")
@RequiredArgsConstructor
public class DayLogController {

    private final DayLogService dayLogService;

    /**
     * Получает список записей времени для заданной даты.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @return список объектов TimeEntryDto
     */
    @GetMapping("/{date}")
    public ResponseEntity<List<TimeEntryDto>> getTimeEntriesByDate(@PathVariable String date) {
        return ResponseEntity.ok(dayLogService.getTimeEntriesByDate(date));
    }

    /**
     * Обновляет или создает запись времени для заданной даты и часа.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @param timeEntryDto объект DTO, содержащий детали записи времени
     * @return обновленный или созданный объект TimeEntryDto
     */
    @PutMapping("/{date}")
    public ResponseEntity<TimeEntryDto> updateTimeEntry(
            @PathVariable String date,
            @RequestBody TimeEntryDto timeEntryDto) {
        return ResponseEntity.ok(dayLogService.updateTimeEntry(date, timeEntryDto));
    }
}