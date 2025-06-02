package com.traker.traker.controller;

import com.traker.traker.dto.CreateUserDto;
import com.traker.traker.dto.UpdateUserDto;
import com.traker.traker.dto.UserDto;
import com.traker.traker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления пользователями.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "API для управления пользователями")
public class UserController {

    private final UserService userService;

    /**
     * Регистрирует нового пользователя и выполняет автоматический логин.
     *
     * @param createUserDto DTO с данными для регистрации
     * @param response      Ответ для установки cookies с токенами
     * @return DTO созданного пользователя
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация пользователя", description = "Создает нового пользователя и автоматически выполняет вход, возвращая его данные и токены.")
    @ApiResponse(responseCode = "201", description = "Пользователь успешно создан", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class)))
    @ApiResponse(responseCode = "400", description = "Неверные данные или пользователь уже существует")
    public UserDto registerUser(@Valid @RequestBody CreateUserDto createUserDto, HttpServletResponse response) {
        return userService.createUser(createUserDto, response);
    }

    /**
     * Обновляет данные текущего пользователя.
     *
     * @param id            ID пользователя
     * @param updateUserDto DTO с данными для обновления
     * @return DTO обновленного пользователя
     */
    @PutMapping("/profile/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Обновление профиля", description = "Обновляет данные текущего пользователя (частично). Доступно только владельцу профиля.")
    @ApiResponse(responseCode = "200", description = "Профиль успешно обновлен", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class)))
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    public UserDto updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDto updateUserDto) {
        return userService.updateUser(id, updateUserDto);
    }

    /**
     * Удаляет текущего пользователя.
     *
     * @param id ID пользователя
     */
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление профиля", description = "Удаляет аккаунт текущего пользователя. Доступно только владельцу профиля.")
    @ApiResponse(responseCode = "204", description = "Профиль успешно удален")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    /**
     * Получает данные текущего аутентифицированного пользователя.
     *
     * @return DTO текущего пользователя
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получение текущего пользователя", description = "Возвращает данные текущего аутентифицированного пользователя.")
    @ApiResponse(responseCode = "200", description = "Данные пользователя получены", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class)))
    @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    public UserDto getCurrentUser() {
        return userService.getCurrentUserDto();
    }
}