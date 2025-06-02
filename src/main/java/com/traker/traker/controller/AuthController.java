package com.traker.traker.controller;

import com.traker.traker.dto.AuthRequestDto;
import com.traker.traker.dto.AuthResponseDto;
import com.traker.traker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для аутентификации и управления токенами")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Аутентификация пользователя", description = "Аутентифицирует пользователя по имени пользователя и паролю, возвращает токены доступа и обновления.")
    @ApiResponse(responseCode = "200", description = "Успешная аутентификация", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Неверные учетные данные")
    public AuthResponseDto login(@Valid @RequestBody AuthRequestDto authRequest, HttpServletResponse response) {
        return authService.login(authRequest, response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токенов", description = "Обновляет токены доступа и обновления на основе переданного refresh-токена.")
    @ApiResponse(responseCode = "200", description = "Токены успешно обновлены", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Недействительный refresh-токен")
    public AuthResponseDto refresh(@CookieValue(name = "refreshToken") String refreshToken, HttpServletResponse response) {
        return authService.refresh(refreshToken, response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы", description = "Очищает токены из cookies для завершения сессии.")
    @ApiResponse(responseCode = "200", description = "Успешный выход из системы")
    public void logout(HttpServletResponse response) {
        authService.logout(response);
    }
}