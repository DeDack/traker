package com.traker.traker.dto.income;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IncomeRecordRequestDto {

    @NotBlank(message = "Название дохода обязательно")
    private String title;

    private String description;

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Сумма должна быть больше нуля")
    private BigDecimal amount;

    @NotNull(message = "Категория обязательна")
    private Long categoryId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate incomeDate;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}$", message = "Период должен быть в формате yyyy-MM")
    private String period;
}
