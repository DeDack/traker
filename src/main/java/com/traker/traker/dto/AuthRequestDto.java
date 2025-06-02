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

    /** Телефон пользователя (используется как логин). */
    @NotBlank(message = "Телефон обязателен")
    private String phone;

    /** Пароль пользователя. */
    @NotBlank(message = "Пароль обязателен")
    private String password;
}