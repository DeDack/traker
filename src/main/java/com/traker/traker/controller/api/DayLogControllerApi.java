package com.traker.traker.controller.api;

import com.traker.traker.dto.TimeEntryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RequestMapping("/api/days")
@Tag(name = "Day Log Controller", description = "Методы для управления записями времени и днями")
public interface DayLogControllerApi {

    @GetMapping("/{date}")
    @Operation(summary = "Получение списка записей времени для заданной даты", description = "Возвращает список объектов TimeEntryDto для указанной даты.")
    @ApiResponse(responseCode = "200", description = "Успешное получение списка записей времени")
    @ApiResponse(responseCode = "400", description = "Некорректный запрос")
    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ResponseEntity<List<TimeEntryDto>> getTimeEntriesByDate(@PathVariable String date);

    @PutMapping("/{date}")
    @Operation(summary = "Обновление или создание записи времени для заданной даты и времени", description = "Обновляет или создает запись времени для указанной даты и конкретного времени (час и минута).")
    @ApiResponse(responseCode = "200", description = "Успешное обновление или создание записи времени")
    @ApiResponse(responseCode = "400", description = "Некорректный запрос")
    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ResponseEntity<TimeEntryDto> updateTimeEntry(@PathVariable String date, @RequestBody TimeEntryDto timeEntryDto);
}