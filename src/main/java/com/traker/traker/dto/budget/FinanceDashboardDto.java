package com.traker.traker.dto.budget;

import com.traker.traker.dto.expense.ExpenseSummaryDto;
import com.traker.traker.dto.income.IncomeSummaryDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceDashboardDto {
    private ExpenseSummaryDto expenseSummary;
    private IncomeSummaryDto incomeSummary;
    private List<BudgetResponseDto> budgets = new ArrayList<>();
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private BigDecimal totalSavingsGoal;
    private BigDecimal savingsProgress;
}
