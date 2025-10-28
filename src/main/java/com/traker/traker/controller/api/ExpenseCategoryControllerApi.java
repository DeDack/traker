package com.traker.traker.controller.api;

import com.traker.traker.dto.expense.ExpenseCategoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Expense Categories", description = "Управление категориями трат")
@RequestMapping("/api/expense-categories")
public interface ExpenseCategoryControllerApi {

    @Operation(summary = "Создать категорию")
    @ApiResponse(responseCode = "200", description = "Категория создана")
    @PostMapping
    ResponseEntity<ExpenseCategoryDto> createCategory(@RequestBody @Valid ExpenseCategoryDto dto);

    @Operation(summary = "Обновить категорию")
    @ApiResponse(responseCode = "200", description = "Категория обновлена")
    @PutMapping("/{id}")
    ResponseEntity<ExpenseCategoryDto> updateCategory(@PathVariable Long id,
                                                      @RequestBody @Valid ExpenseCategoryDto dto);

    @Operation(summary = "Удалить категорию")
    @ApiResponse(responseCode = "204", description = "Категория удалена")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteCategory(@PathVariable Long id);

    @Operation(summary = "Получить список категорий")
    @ApiResponse(responseCode = "200", description = "Категории получены")
    @GetMapping
    ResponseEntity<List<ExpenseCategoryDto>> getCategories();
}
