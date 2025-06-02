package com.traker.traker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO для представления пользователя.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    /** Уникальный идентификатор пользователя. */
    private Long id;

    /** Имя пользователя (username). */
    @Size(max = 50, message = "Имя пользователя не должно превышать 50 символов")
    private String username;

    /** Имя пользователя. */
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    private String name;

    /** Дата и время регистрации. */
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления. */
    private LocalDateTime updatedAt;

    /** Роли пользователя. */
    private Set<RoleDto> roles;
}
