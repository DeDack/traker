package com.traker.traker.controller.api;

import com.traker.traker.dto.budget.BudgetRequestDto;
import com.traker.traker.dto.budget.BudgetResponseDto;
import com.traker.traker.dto.budget.FinanceDashboardDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Budgets", description = "Планирование бюджета и финансовый дашборд")
@RequestMapping("/api/budgets")
public interface BudgetControllerApi {

    @Operation(summary = "Создать или обновить бюджет")
    @ApiResponse(responseCode = "200", description = "Бюджет сохранен")
    @PostMapping
    ResponseEntity<BudgetResponseDto> upsert(@RequestBody @Valid BudgetRequestDto request);

    @Operation(summary = "Получить бюджет по месяцу")
    @ApiResponse(responseCode = "200", description = "Бюджет получен")
    @GetMapping("/{month}")
    ResponseEntity<BudgetResponseDto> getBudget(@PathVariable String month);

    @Operation(summary = "Получить бюджеты по фильтру")
    @ApiResponse(responseCode = "200", description = "Бюджеты получены")
    @GetMapping
    ResponseEntity<List<BudgetResponseDto>> getBudgets(@RequestParam(required = false) String from,
                                                       @RequestParam(required = false) String to,
                                                       @RequestParam(required = false) String month);

    @Operation(summary = "Получить финансовый дашборд")
    @ApiResponse(responseCode = "200", description = "Дашборд сформирован")
    @GetMapping("/dashboard")
    ResponseEntity<FinanceDashboardDto> getDashboard(@RequestParam(required = false) String from,
                                                     @RequestParam(required = false) String to,
                                                     @RequestParam(required = false) String month,
                                                     @RequestParam(required = false, name = "expenseCategories") List<Long> expenseCategoryIds,
                                                     @RequestParam(required = false, name = "incomeCategories") List<Long> incomeCategoryIds);
}
