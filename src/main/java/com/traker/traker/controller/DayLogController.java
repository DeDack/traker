package com.traker.traker.controller;

import com.traker.traker.controller.api.DayLogControllerApi;
import com.traker.traker.dto.TimeEntryDto;
import com.traker.traker.service.DayLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для управления записями времени и днями.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class DayLogController implements DayLogControllerApi {

    private final DayLogService dayLogService;

    /**
     * Получает список записей времени для заданной даты.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @return список объектов TimeEntryDto
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimeEntryDto>> getTimeEntriesByDate(String date) {
        log.info("Получение списка записей времени для даты: {}", date);
        return ResponseEntity.ok(dayLogService.getTimeEntriesByDate(date));
    }

    /**
     * Обновляет или создает запись времени для заданной даты и часа.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @param timeEntryDto объект DTO, содержащий детали записи времени
     * @return обновленный или созданный объект TimeEntryDto
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TimeEntryDto> updateTimeEntry(String date, TimeEntryDto timeEntryDto) {
        log.info("Обновление или создание записи времени для даты: {}, данные: {}", date, timeEntryDto);
        return ResponseEntity.ok(dayLogService.updateTimeEntry(date, timeEntryDto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTimeEntry(String date, int hour, int minute) {
        log.info("Удаление записи времени для даты: {}, время: {}:{}", date, hour, minute);
        dayLogService.deleteTimeEntry(date, hour, minute);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTimeEntry(Long id) {
        log.info("Удаление записи времени по идентификатору: {}", id);
        dayLogService.deleteTimeEntry(id);
        return ResponseEntity.noContent().build();
    }
}