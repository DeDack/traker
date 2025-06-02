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

    /** Телефон пользователя. */
    @Size(max = 20, message = "Телефон не должен превышать 20 символов")
    private String phone;

    /** Имя пользователя. */
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    private String name;

    /** Email пользователя. */
    @Email(message = "Email должен быть валидным")
    @Size(max = 255, message = "Email не должен превышать 255 символов")
    private String email;

    /** Ссылка на профиль Telegram. */
    @Size(max = 255, message = "Ссылка на Telegram не должна превышать 255 символов")
    private String telegram;

    /** Ссылка на профиль WhatsApp. */
    @Size(max = 255, message = "Ссылка на WhatsApp не должна превышать 255 символов")
    private String whatsapp;

    /** Дата и время регистрации. */
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления. */
    private LocalDateTime updatedAt;

    /** Роли пользователя. */
    private Set<RoleDto> roles;
}