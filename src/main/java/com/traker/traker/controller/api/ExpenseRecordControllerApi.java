package com.traker.traker.controller.api;

import com.traker.traker.dto.common.BulkIdRequestDto;
import com.traker.traker.dto.expense.ExpenseBatchCreateRequestDto;
import com.traker.traker.dto.expense.ExpenseBatchUpdateRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordResponseDto;
import com.traker.traker.dto.expense.ExpenseSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

    @Operation(summary = "Обновление траты")
    @ApiResponse(responseCode = "200", description = "Трата обновлена")
    @PutMapping("/{id}")
    ResponseEntity<ExpenseRecordResponseDto> updateExpense(@PathVariable Long id,
                                                           @RequestBody @Valid ExpenseRecordRequestDto request);

    @Operation(summary = "Массовое обновление трат")
    @ApiResponse(responseCode = "200", description = "Траты обновлены")
    @PutMapping("/bulk")
    ResponseEntity<List<ExpenseRecordResponseDto>> updateExpenses(@RequestBody @Valid ExpenseBatchUpdateRequestDto request);

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

    @Operation(summary = "Удаление траты")
    @ApiResponse(responseCode = "204", description = "Трата удалена")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteExpense(@PathVariable Long id);

    @Operation(summary = "Массовое удаление трат")
    @ApiResponse(responseCode = "204", description = "Траты удалены")
    @PostMapping("/bulk-delete")
    ResponseEntity<Void> deleteExpenses(@RequestBody @Valid BulkIdRequestDto request);
}
