package com.example.tomcat.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Plataforma de Renta de Herramientas y Equipos - API",
        version = "1.0.0",
        description = "Documentación oficial de la API backend para la gestión de alquiler de equipos, usuarios, reservas y pagos."
    ),
    security = { @SecurityRequirement(name = "bearerAuth") }
)
@SecurityScheme(
    name = "bearerAuth",
    description = "Autenticación JWT usando el formato Bearer. Ejemplo: 'Bearer eyJhbGciOi...'",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}