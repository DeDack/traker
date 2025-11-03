package com.traker.traker.dto.expense;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRecordResponseDto {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private LocalDate expenseDate;
    private String period;
    private boolean monthOnly;
}
