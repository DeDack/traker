package com.traker.traker.dto.income;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IncomeRecordResponseDto {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private LocalDate incomeDate;
    private String period;
    private boolean monthOnly;
}
