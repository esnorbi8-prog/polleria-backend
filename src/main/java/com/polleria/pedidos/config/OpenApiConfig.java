package com.polleria.pedidos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agrega el botón "Authorize" en Swagger UI: después de hacer login en
 * POST /api/auth/login, se pega el token ahí (sin escribir "Bearer ", Swagger
 * lo agrega solo) y todos los "Try it out" siguientes ya salen autenticados.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI polleriaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Pollería — Sistema Web Integrado (Caso 2)")
                        .description("Pedidos, pagos (Mercado Pago) y ahora usuarios/roles (avance 3).")
                        .version("v0.3"))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
