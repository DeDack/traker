package com.traker.traker.dto.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class ExpenseBatchCreateRequestDto {

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}$", message = "Период должен быть в формате yyyy-MM")
    private String defaultPeriod;

    @NotEmpty(message = "Список трат не может быть пустым")
    @Valid
    private List<ExpenseRecordRequestDto> expenses;
}
