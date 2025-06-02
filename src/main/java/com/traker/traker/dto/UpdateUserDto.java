package com.traker.traker.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDto {
    private String name;

    @Size(min = 6, message = "Пароль должен содержать не менее 6 символов")
    private String password;

    private String username;
}
