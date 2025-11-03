package com.traker.traker.dto.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ExpenseBatchUpdateRequestDto {

    @NotEmpty(message = "Список обновлений не может быть пустым")
    @Valid
    private List<ExpenseRecordUpdateDto> records;
}
