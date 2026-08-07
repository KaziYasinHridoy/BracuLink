package com.braculink.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the bearer JWT scheme with springdoc, so {@code /swagger-ui/index.html} shows an
 * "Authorize" button and every operation can be tried against a real, running server without
 * hand-crafting an Authorization header. {@code /api/auth/**} endpoints override this per-operation
 * to show as not requiring it — see {@link com.braculink.controller.AuthController}.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Braculink API",
                description = "BRACU section-swap coordination: routines, friends, and matching.",
                version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
@Configuration
public class OpenApiConfig {
}
