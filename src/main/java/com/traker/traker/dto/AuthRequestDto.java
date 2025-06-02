package com.traker.traker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса авторизации (логин и пароль).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDto {

    /** Имя пользователя (используется как логин). */
    @NotBlank(message = "Имя пользователя обязательно")
    private String username;

    /** Пароль пользователя. */
    @NotBlank(message = "Пароль обязателен")
    private String password;
}
