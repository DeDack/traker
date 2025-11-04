package com.traker.traker.dto.income;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class IncomeBatchCreateRequestDto {

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}$", message = "Период должен быть в формате yyyy-MM")
    private String defaultPeriod;

    @NotEmpty(message = "Список доходов не может быть пустым")
    @Valid
    private List<IncomeRecordRequestDto> incomes;
}
