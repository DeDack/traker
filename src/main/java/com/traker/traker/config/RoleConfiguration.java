package com.traker.traker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class RoleConfiguration {

    public void configureAuthorization(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // Публичные эндпоинты, не требующие авторизации
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/users/register",
                        "/",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/swagger",
                        "/favicon.ico"
                ).permitAll()

                // Эндпоинты управления пользователями — доступ только USER и выше
                .requestMatchers(
                        "/api/users/profile/**",
                        "/api/users/me",
                        "/api/users/delete/**",
                        "/api/users/register"
                ).hasAnyRole("USER", "MODERATOR", "ADMIN")

                // Эндпоинты для статусов — доступ USER и выше
                .requestMatchers(
                        "/api/statuses/**"
                ).hasAnyRole("USER", "MODERATOR", "ADMIN")

                // Эндпоинты для управления записями времени — доступ USER и выше
                .requestMatchers(
                        "/api/stats/daily",
                        "/api/days/**"
                ).hasAnyRole("USER", "MODERATOR", "ADMIN")

                // Остальные запросы требуют авторизации
                .anyRequest().authenticated()
        );
    }
}