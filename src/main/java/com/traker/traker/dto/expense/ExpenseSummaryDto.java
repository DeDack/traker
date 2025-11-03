package com.traker.traker.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryDto {
    private BigDecimal totalAmount;
    private List<CategoryTotalDto> totalsByCategory = new ArrayList<>();
    private List<MonthlyTotalDto> totalsByMonth = new ArrayList<>();
    private List<CategoryMonthlySummaryDto> categoryMonthlyTotals = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTotalDto {
        private Long categoryId;
        private String categoryName;
        private BigDecimal totalAmount;
        private BigDecimal percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTotalDto {
        private String period;
        private BigDecimal totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMonthlySummaryDto {
        private Long categoryId;
        private String categoryName;
        private List<MonthlyTotalDto> monthlyTotals = new ArrayList<>();
    }
}
