package com.traker.traker.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Time Entry Controller", description = "Методы для управления записями времени")
public interface TimeEntryControllerApi {

    @GetMapping("/api/stats/daily")
    @Operation(summary = "Отправка статуса открытия счёта", description = "Отправляет статус открытия счёта во временный кабинет ДБО ЮЛ (ЦОС).")
    @ApiResponse(responseCode = "200", description = "Успешная отправка статуса открытия счёта")
    @ApiResponse(responseCode = "400", description = "Некорректный запрос")
    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    int getDailyStats(@RequestParam("date") String date);
}