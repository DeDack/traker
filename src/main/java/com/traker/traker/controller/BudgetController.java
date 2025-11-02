package com.traker.traker.controller;

import com.traker.traker.controller.api.BudgetControllerApi;
import com.traker.traker.dto.budget.BudgetRequestDto;
import com.traker.traker.dto.budget.BudgetResponseDto;
import com.traker.traker.dto.budget.FinanceDashboardDto;
import com.traker.traker.service.BudgetService;
import com.traker.traker.service.FinanceSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BudgetController implements BudgetControllerApi {

    private final BudgetService budgetService;
    private final FinanceSummaryService financeSummaryService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BudgetResponseDto> upsert(@RequestBody @Valid BudgetRequestDto request) {
        return ResponseEntity.ok(budgetService.upsertBudget(request));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BudgetResponseDto> getBudget(@PathVariable String month) {
        return ResponseEntity.ok(budgetService.getBudget(month));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BudgetResponseDto>> getBudgets(@RequestParam(required = false) String from,
                                                              @RequestParam(required = false) String to,
                                                              @RequestParam(required = false) String month) {
        return ResponseEntity.ok(budgetService.getBudgets(from, to, month));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanceDashboardDto> getDashboard(@RequestParam(required = false) String from,
                                                            @RequestParam(required = false) String to,
                                                            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(financeSummaryService.getDashboard(from, to, month));
    }
}
