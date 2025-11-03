package com.traker.traker.controller.api;

import com.traker.traker.dto.income.IncomeCategoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Income Categories", description = "Управление категориями доходов")
@RequestMapping("/api/income-categories")
public interface IncomeCategoryControllerApi {

    @Operation(summary = "Создать категорию доходов")
    @ApiResponse(responseCode = "200", description = "Категория создана")
    @PostMapping
    ResponseEntity<IncomeCategoryDto> createCategory(@RequestBody @Valid IncomeCategoryDto dto);

    @Operation(summary = "Обновить категорию доходов")
    @ApiResponse(responseCode = "200", description = "Категория обновлена")
    @PutMapping("/{id}")
    ResponseEntity<IncomeCategoryDto> updateCategory(@PathVariable Long id,
                                                     @RequestBody @Valid IncomeCategoryDto dto);

    @Operation(summary = "Удалить категорию доходов")
    @ApiResponse(responseCode = "204", description = "Категория удалена")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteCategory(@PathVariable Long id);

    @Operation(summary = "Получить список категорий доходов")
    @ApiResponse(responseCode = "200", description = "Категории получены")
    @GetMapping
    ResponseEntity<List<IncomeCategoryDto>> getCategories();
}
