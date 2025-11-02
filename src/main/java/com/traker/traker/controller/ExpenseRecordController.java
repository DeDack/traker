package com.traker.traker.controller;

import com.traker.traker.controller.api.ExpenseRecordControllerApi;
import com.traker.traker.dto.expense.ExpenseBatchCreateRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordResponseDto;
import com.traker.traker.dto.expense.ExpenseSummaryDto;
import com.traker.traker.service.ExpenseRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExpenseRecordController implements ExpenseRecordControllerApi {

    private final ExpenseRecordService expenseRecordService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExpenseRecordResponseDto>> createBatch(@RequestBody @Valid ExpenseBatchCreateRequestDto request) {
        return ResponseEntity.ok(expenseRecordService.createBatch(request));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExpenseRecordResponseDto>> getExpenses(@RequestParam(required = false) String from,
                                                                      @RequestParam(required = false) String to,
                                                                      @RequestParam(required = false) String month,
                                                                      @RequestParam(required = false, name = "categories") List<Long> categoryIds) {
        return ResponseEntity.ok(expenseRecordService.getExpenses(from, to, month, categoryIds));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseSummaryDto> getSummary(@RequestParam(required = false) String from,
                                                        @RequestParam(required = false) String to,
                                                        @RequestParam(required = false) String month,
                                                        @RequestParam(required = false, name = "categories") List<Long> categoryIds) {
        return ResponseEntity.ok(expenseRecordService.getSummary(from, to, month, categoryIds));
    }
}
