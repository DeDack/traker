package com.traker.traker.controller.api;

import com.traker.traker.dto.common.BulkIdRequestDto;
import com.traker.traker.dto.income.IncomeBatchCreateRequestDto;
import com.traker.traker.dto.income.IncomeBatchUpdateRequestDto;
import com.traker.traker.dto.income.IncomeRecordRequestDto;
import com.traker.traker.dto.income.IncomeRecordResponseDto;
import com.traker.traker.dto.income.IncomeSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Income Records", description = "Управление доходами и отчетами")
@RequestMapping("/api/incomes")
public interface IncomeRecordControllerApi {

    @Operation(summary = "Массовое создание доходов")
    @ApiResponse(responseCode = "200", description = "Доходы сохранены")
    @PostMapping("/batch")
    ResponseEntity<List<IncomeRecordResponseDto>> createBatch(@RequestBody @Valid IncomeBatchCreateRequestDto request);

    @Operation(summary = "Обновление дохода")
    @ApiResponse(responseCode = "200", description = "Доход обновлен")
    @PutMapping("/{id}")
    ResponseEntity<IncomeRecordResponseDto> updateIncome(@PathVariable Long id,
                                                         @RequestBody @Valid IncomeRecordRequestDto request);

    @Operation(summary = "Массовое обновление доходов")
    @ApiResponse(responseCode = "200", description = "Доходы обновлены")
    @PutMapping("/bulk")
    ResponseEntity<List<IncomeRecordResponseDto>> updateIncomes(@RequestBody @Valid IncomeBatchUpdateRequestDto request);

    @Operation(summary = "Получение доходов по фильтру")
    @ApiResponse(responseCode = "200", description = "Доходы получены")
    @GetMapping
    ResponseEntity<List<IncomeRecordResponseDto>> getIncomes(@RequestParam(required = false) String from,
                                                             @RequestParam(required = false) String to,
                                                             @RequestParam(required = false) String month,
                                                             @RequestParam(required = false, name = "categories") List<Long> categoryIds);

    @Operation(summary = "Получение агрегированной статистики по доходам")
    @ApiResponse(responseCode = "200", description = "Статистика сформирована")
    @GetMapping("/summary")
    ResponseEntity<IncomeSummaryDto> getSummary(@RequestParam(required = false) String from,
                                                @RequestParam(required = false) String to,
                                                @RequestParam(required = false) String month,
                                                @RequestParam(required = false, name = "categories") List<Long> categoryIds);

    @Operation(summary = "Удаление дохода")
    @ApiResponse(responseCode = "204", description = "Доход удален")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteIncome(@PathVariable Long id);

    @Operation(summary = "Массовое удаление доходов")
    @ApiResponse(responseCode = "204", description = "Доходы удалены")
    @PostMapping("/bulk-delete")
    ResponseEntity<Void> deleteIncomes(@RequestBody @Valid BulkIdRequestDto request);
}
