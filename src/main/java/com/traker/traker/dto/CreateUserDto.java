package com.traker.traker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDto {
    @NotBlank(message = "Номер телефона обязателен")
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Номер телефона должен быть в формате +79223332211")
    private String phone;

    @NotBlank(message = "Имя обязательно")
    private String name;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать не менее 6 символов")
    private String password;

}