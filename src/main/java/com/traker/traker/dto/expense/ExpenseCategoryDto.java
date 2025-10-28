package com.traker.traker.dto.expense;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseCategoryDto {
    private Long id;

    @NotBlank(message = "Название категории не может быть пустым")
    private String name;

    private String description;
}
