package com.traker.traker.controller.api;


import com.traker.traker.dto.StatusDto;
import com.traker.traker.entity.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/statuses")
public interface StatusControllerApi {

    @Operation(summary = "Создать новый статус", description = "Создает новый статус в системе.")
    @ApiResponse(responseCode = "200", description = "Статус успешно создан")
    @ApiResponse(responseCode = "400", description = "Некорректные данные")

    @PostMapping("/createStatus")
    ResponseEntity<StatusDto> createStatus(@RequestBody StatusDto statusDto);

    @Operation(summary = "Найти статус по имени", description = "Возвращает статус по указанному имени.")
    @ApiResponse(responseCode = "200", description = "Статус найден")
    @ApiResponse(responseCode = "404", description = "Статус не найден")
    @GetMapping("/findByName/{name}")
    ResponseEntity<StatusDto> findByName(@Parameter(description = "Имя статуса") @PathVariable String name);

    @Operation(summary = "Обновить статус по ID", description = "Обновляет существующий статус по указанному ID.")
    @ApiResponse(responseCode = "200", description = "Статус успешно обновлен")
    @ApiResponse(responseCode = "404", description = "Статус не найден")
    @PutMapping("/updateStatus/{id}")
    ResponseEntity<StatusDto> updateStatus(@Parameter(description = "ID статуса") @PathVariable Long id,
                                           @RequestBody StatusDto statusDto);

    @Operation(summary = "Удалить статус по ID", description = "Удаляет статус по указанному ID.")
    @ApiResponse(responseCode = "204", description = "Статус успешно удален")
    @ApiResponse(responseCode = "404", description = "Статус не найден")
    @DeleteMapping("/deleteStatus/{id}")
    ResponseEntity<Void> deleteStatus(@Parameter(description = "ID статуса") @PathVariable Long id);

    @Operation(summary = "Получить все статусы", description = "Возвращает список всех статусов в системе.")
    @ApiResponse(responseCode = "200", description = "Список статусов возвращен")
    @GetMapping("/getAllStatuses")
    ResponseEntity<List<StatusDto>> getAllStatuses();

    @Operation(summary = "Получить статус по ID", description = "Возвращает статус по указанному ID.")
    @ApiResponse(responseCode = "200", description = "Статус найден")
    @ApiResponse(responseCode = "404", description = "Статус не найден")
    @GetMapping("/getStatusById/{id}")
    ResponseEntity<Status> getStatusById(@Parameter(description = "ID статуса") @PathVariable Long id);
}
