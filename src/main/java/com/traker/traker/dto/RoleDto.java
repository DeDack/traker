package com.traker.traker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для представления роли.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {

    /** Уникальный идентификатор роли. */
    private Long id;

    /** Название роли. */
    @NotNull(message = "Название роли обязательно")
    @Size(max = 50, message = "Название роли не должно превышать 50 символов")
    private String name;
}