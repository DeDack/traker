package com.traker.traker.service;

import com.traker.traker.dto.budget.BudgetResponseDto;
import com.traker.traker.dto.budget.FinanceDashboardDto;
import com.traker.traker.dto.expense.ExpenseSummaryDto;
import com.traker.traker.dto.income.IncomeSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static com.traker.traker.utils.FinanceUtils.normalizeAmount;

@Service
@RequiredArgsConstructor
public class FinanceSummaryService {

    private final ExpenseRecordService expenseRecordService;
    private final IncomeRecordService incomeRecordService;
    private final BudgetService budgetService;

    public FinanceDashboardDto getDashboard(String fromDate, String toDate, String month) {
        ExpenseSummaryDto expenseSummary = expenseRecordService.getSummary(fromDate, toDate, month);
        IncomeSummaryDto incomeSummary = incomeRecordService.getSummary(fromDate, toDate, month);
        List<BudgetResponseDto> budgets = budgetService.getBudgets(fromDate, toDate, month);

        FinanceDashboardDto dashboard = new FinanceDashboardDto();
        dashboard.setExpenseSummary(expenseSummary);
        dashboard.setIncomeSummary(incomeSummary);
        dashboard.setBudgets(budgets);

        BigDecimal totalExpenses = normalizeAmount(expenseSummary.getTotalAmount());
        BigDecimal totalIncome = normalizeAmount(incomeSummary.getTotalAmount());
        dashboard.setTotalExpenses(totalExpenses);
        dashboard.setTotalIncome(totalIncome);
        dashboard.setNetBalance(totalIncome.subtract(totalExpenses));

        BigDecimal totalSavingsGoal = budgets.stream()
                .map(BudgetResponseDto::getSavingsGoal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal savingsProgress = budgets.stream()
                .map(BudgetResponseDto::getSavingsProgress)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboard.setTotalSavingsGoal(normalizeAmount(totalSavingsGoal));
        dashboard.setSavingsProgress(normalizeAmount(savingsProgress));
        return dashboard;
    }
}
