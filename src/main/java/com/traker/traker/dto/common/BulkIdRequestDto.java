package com.traker.traker.dto.common;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkIdRequestDto {

    @NotEmpty(message = "Нужно указать хотя бы один идентификатор")
    private List<Long> ids;
}
