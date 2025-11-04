package com.traker.traker.dto.budget;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetResponseDto {
    private Long id;
    private String month;
    private BigDecimal plannedIncome;
    private BigDecimal plannedExpense;
    private BigDecimal savingsGoal;
    private BigDecimal actualIncome;
    private BigDecimal actualExpense;
    private BigDecimal plannedBalance;
    private BigDecimal actualBalance;
    private BigDecimal remainingToSpend;
    private BigDecimal savingsProgress;
    private String notes;
}
