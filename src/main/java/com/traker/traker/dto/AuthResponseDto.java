package com.traker.traker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа на запрос авторизации.
 * Содержит access-токен, refresh-токен и метаданные в формате, совместимом с OAuth2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    /** Access-токен (JWT) для аутентификации запросов. */
    private String access_token;

    /** Тип токена (по умолчанию "Bearer"). */
    private String token_type;

    /** Refresh-токен для обновления access-токена. */
    private String refresh_token;

    /** Время жизни access-токена в секундах. */
    private long expires_in;

    /** Время жизни refresh-токена в секундах. */
    private long refresh_expires_in;

    /** Область действия токена (scopes). */
    private String scope;

    /** Уникальный идентификатор токена (JTI). */
    private String jti;
}