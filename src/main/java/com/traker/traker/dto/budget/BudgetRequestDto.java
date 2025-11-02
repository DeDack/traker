package com.traker.traker.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequestDto {

    @NotBlank(message = "Месяц обязателен")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Месяц должен быть в формате yyyy-MM")
    private String month;

    @DecimalMin(value = "0.0", message = "Планируемый доход не может быть отрицательным")
    private BigDecimal plannedIncome;

    @DecimalMin(value = "0.0", message = "Планируемые расходы не могут быть отрицательными")
    private BigDecimal plannedExpense;

    @DecimalMin(value = "0.0", message = "Цель по сбережениям не может быть отрицательной")
    private BigDecimal savingsGoal;

    private String notes;
}
