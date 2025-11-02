package com.traker.traker.controller.api;

import com.traker.traker.dto.expense.ExpenseBatchCreateRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordResponseDto;
import com.traker.traker.dto.expense.ExpenseSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Expense Records", description = "Управление тратами и отчетами")
@RequestMapping("/api/expenses")
public interface ExpenseRecordControllerApi {

    @Operation(summary = "Массовое создание трат")
    @ApiResponse(responseCode = "200", description = "Траты сохранены")
    @PostMapping("/batch")
    ResponseEntity<List<ExpenseRecordResponseDto>> createBatch(@RequestBody @Valid ExpenseBatchCreateRequestDto request);

    @Operation(summary = "Получение трат по фильтру")
    @ApiResponse(responseCode = "200", description = "Траты получены")
    @GetMapping
    ResponseEntity<List<ExpenseRecordResponseDto>> getExpenses(@RequestParam(required = false) String from,
                                                               @RequestParam(required = false) String to,
                                                               @RequestParam(required = false) String month,
                                                               @RequestParam(required = false, name = "categories") List<Long> categoryIds);

    @Operation(summary = "Получение агрегированной статистики")
    @ApiResponse(responseCode = "200", description = "Статистика сформирована")
    @GetMapping("/summary")
    ResponseEntity<ExpenseSummaryDto> getSummary(@RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String to,
                                                 @RequestParam(required = false) String month,
                                                 @RequestParam(required = false, name = "categories") List<Long> categoryIds);
}
