package com.traker.traker.dto.income;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IncomeBatchUpdateRequestDto {

    @NotEmpty(message = "Список обновлений не может быть пустым")
    @Valid
    private List<IncomeRecordUpdateDto> records;
}
