package com.traker.traker.controller.api;

import com.traker.traker.dto.income.IncomeBatchCreateRequestDto;
import com.traker.traker.dto.income.IncomeRecordResponseDto;
import com.traker.traker.dto.income.IncomeSummaryDto;
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

@Tag(name = "Income Records", description = "Управление доходами и отчетами")
@RequestMapping("/api/incomes")
public interface IncomeRecordControllerApi {

    @Operation(summary = "Массовое создание доходов")
    @ApiResponse(responseCode = "200", description = "Доходы сохранены")
    @PostMapping("/batch")
    ResponseEntity<List<IncomeRecordResponseDto>> createBatch(@RequestBody @Valid IncomeBatchCreateRequestDto request);

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
}
