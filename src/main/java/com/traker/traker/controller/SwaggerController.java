package com.traker.traker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequiredArgsConstructor
@Tag(name = "Swagger Controller", description = "Методы для перенаправления на Swagger UI")
public class SwaggerController {

    @GetMapping("/sw")
    @Operation(
            summary = "Перенаправление на Swagger UI",
            description = "Перенаправляет на страницу Swagger UI по пути /api/doc."
    )
    @ApiResponse(responseCode = "302", description = "Перенаправление на /api/doc")
    public ResponseEntity<Void> redirectToSwaggerUI() {
        return ResponseEntity
                .status(302)
                .header("Location", "/swagger-ui/index.html#/")
                .build();
    }
}