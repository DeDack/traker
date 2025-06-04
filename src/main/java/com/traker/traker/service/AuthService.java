package com.traker.traker.service;

import com.traker.traker.config.JwtUtil;
import com.traker.traker.dto.AuthRequestDto;
import com.traker.traker.dto.AuthResponseDto;
import com.traker.traker.exception.InvalidTokenException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Сервис для управления аутентификацией пользователей.
 * <p>
 * Предоставляет методы для входа в систему, обновления токенов и выхода из системы.
 * Использует JWT для генерации и валидации токенов.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Value("${auth.token_type}")
    private String tokenType;

    @Value("${auth.scope}")
    private String scope;

    /**
     * Аутентифицирует пользователя по имени пользователя и паролю.
     * <p>
     * При успешной аутентификации генерирует токены доступа и обновления,
     * устанавливает их в cookies и возвращает информацию о токенах.
     * </p>
     *
     * @param authRequest DTO с именем пользователя и паролем пользователя.
     * @param response    Объект HttpServletResponse для установки cookies.
     * @return DTO с информацией о токенах.
     */
    public AuthResponseDto login(AuthRequestDto authRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        JwtUtil.TokenInfo accessTokenInfo = jwtUtil.createAccessToken(userDetails);
        JwtUtil.TokenInfo refreshTokenInfo = jwtUtil.createRefreshToken(userDetails);

        setTokenCookies(response, accessTokenInfo, refreshTokenInfo);

        return new AuthResponseDto(
                accessTokenInfo.getToken(),
                tokenType,
                refreshTokenInfo.getToken(),
                accessTokenInfo.getExpiresIn(),
                refreshTokenInfo.getExpiresIn(),
                scope,
                accessTokenInfo.getJti()
        );
    }

    /**
     * Обновляет токены доступа и обновления на основе refresh-токена.
     * <p>
     * Проверяет валидность refresh-токена, генерирует новые токены,
     * устанавливает их в cookies и возвращает информацию о новых токенах.
     * </p>
     *
     * @param refreshToken Refresh-токен из cookies.
     * @param response     Объект HttpServletResponse для установки новых cookies.
     * @return DTO с информацией о новых токенах.
     * @throws InvalidTokenException Если refresh-токен недействителен или истёк.
     */
    public AuthResponseDto refresh(String refreshToken, HttpServletResponse response) {
        logger.info("Получен рефреш-токен: {}", refreshToken);
        if (!jwtUtil.validateToken(refreshToken)) {
            logger.error("Невалидный рефреш-токен: {}", refreshToken);
            throw new InvalidTokenException("Недействительный или истекший refresh-токен");
        }

        String username = jwtUtil.getUsername(refreshToken);
        logger.info("Извлечён username: {}", username);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        JwtUtil.TokenInfo accessTokenInfo = jwtUtil.createAccessToken(userDetails);
        JwtUtil.TokenInfo newRefreshTokenInfo = jwtUtil.createRefreshToken(userDetails);

        setTokenCookies(response, accessTokenInfo, newRefreshTokenInfo);

        return new AuthResponseDto(
                accessTokenInfo.getToken(),
                tokenType,
                newRefreshTokenInfo.getToken(),
                accessTokenInfo.getExpiresIn(),
                newRefreshTokenInfo.getExpiresIn(),
                scope,
                accessTokenInfo.getJti()
        );
    }

    /**
     * Выполняет выход из системы, очищая cookies с токенами.
     *
     * @param response Объект HttpServletResponse для удаления cookies.
     */
    public void logout(HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie.from(ACCESS_TOKEN, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    /**
     * Устанавливает cookies с токенами доступа и обновления.
     *
     * @param response          Объект HttpServletResponse для установки cookies.
     * @param accessTokenInfo   Информация о токене доступа.
     * @param refreshTokenInfo  Информация о токене обновления.
     */
    private void setTokenCookies(HttpServletResponse response, JwtUtil.TokenInfo accessTokenInfo, JwtUtil.TokenInfo refreshTokenInfo) {
        ResponseCookie accessTokenCookie = ResponseCookie.from(ACCESS_TOKEN, accessTokenInfo.getToken())
                .httpOnly(true)
                .path("/")
                .maxAge(accessTokenInfo.getExpiresIn())
                .sameSite("Strict")
                .build();
        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN, refreshTokenInfo.getToken())
                .httpOnly(true)
                .path("/")
                .maxAge(refreshTokenInfo.getExpiresIn())
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}