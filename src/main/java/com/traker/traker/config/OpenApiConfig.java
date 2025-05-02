package com.traker.traker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Сервис мониторинга активностей",
                version = "1.0",
                license = @License(
                        name = "Все права защищены"
                )
        )
)
@SecurityScheme(
    name = "accessToken",
    type = SecuritySchemeType.APIKEY
)
@Configuration
public class OpenApiConfig {
}