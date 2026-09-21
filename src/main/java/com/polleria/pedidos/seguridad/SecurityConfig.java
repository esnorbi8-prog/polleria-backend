package com.polleria.pedidos.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración central de seguridad (RF-A01): API sin sesiones, autenticada
 * por JWT, con reglas de autorización por rol para cada módulo.
 *
 * Reparto de permisos, en resumen:
 * - Público: registro/login/verificación, catálogo de productos (GET),
 *   Swagger, consola H2 y el webhook de Mercado Pago (lo llama Mercado Pago,
 *   no un usuario logueado, así que no puede exigir token).
 * - CLIENTE: crear sus pedidos, iniciar/confirmar su pago, ver sus cosas.
 * - COCINERO / REPARTIDOR: cambiar el estado de los pedidos (RF-K02/RF-D03).
 * - CAJERA / ADMINISTRADOR: productos (alta/edición), clientes, y todo lo
 *   anterior también (para atender pedidos por teléfono / mostrador).
 * - ADMINISTRADOR: gestión de cuentas de personal (/api/usuarios/**).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Reemplaza al viejo WebMvcConfigurer de CORS: con Spring Security activo,
     * el CORS se resuelve acá (antes de que el filtro de seguridad rechace un
     * preflight), así el frontend Angular puede seguir llamando a la API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Spring Security arma automáticamente el DaoAuthenticationProvider a
     * partir del bean CustomUserDetailsService + el PasswordEncoder de
     * arriba; no hace falta declararlo a mano.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // La consola H2 se sirve dentro de un <frame>; sameOrigin() la deja
                // funcionar sin abrir la protección a orígenes externos.
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        // Documentación y herramientas de desarrollo
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/h2-console/**")
                        .permitAll()
                        // Cuenta / login (RF-C01, RF-C02)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Mercado Pago llama este endpoint solo, sin JWT
                        .requestMatchers("/api/pagos/webhook").permitAll()
                        // Catálogo público (RF-C03)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/productos/**").permitAll()

                        // Gestión de personal: solo administrador (RF-A01)
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")

                        // Productos: alta/edición reservada a caja/admin (RF-A03)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/productos/**")
                        .hasAnyRole("CAJERA", "ADMINISTRADOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/productos/**")
                        .hasAnyRole("CAJERA", "ADMINISTRADOR")

                        // Clientes: consulta/alta manual reservada a caja/admin, salvo /me que es del propio cliente
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/clientes/me").hasAnyRole("CLIENTE", "CAJERA", "ADMINISTRADOR")
                        .requestMatchers("/api/clientes/**").hasAnyRole("CAJERA", "ADMINISTRADOR")

                        // Cambiar el estado de un pedido: cocina, reparto, caja o admin
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/pedidos/*/estado")
                        .hasAnyRole("COCINERO", "REPARTIDOR", "CAJERA", "ADMINISTRADOR")

                        // Crear pedido / pagar: el propio cliente, o caja/admin (pedido presencial)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/pedidos")
                        .hasAnyRole("CLIENTE", "CAJERA", "ADMINISTRADOR")
                        .requestMatchers("/api/pedidos/*/pago", "/api/pedidos/*/pago/confirmar")
                        .hasAnyRole("CLIENTE", "CAJERA", "ADMINISTRADOR")

                        // Cualquier otro endpoint: basta con estar logueado (cualquier rol)
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(this::manejarNoAutenticado)
                        .accessDeniedHandler(this::manejarSinPermiso))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 401: no hay token, o el token es inválido/expiró. Mismo formato de error que el resto de la API. */
    private void manejarNoAutenticado(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
                                       org.springframework.security.core.AuthenticationException ex) throws java.io.IOException {
        escribirError(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado: falta un token válido (Authorization: Bearer <token>)");
    }

    /** 403: el token es válido, pero el rol del usuario no tiene permiso para este endpoint. */
    private void manejarSinPermiso(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException ex) throws java.io.IOException {
        escribirError(response, HttpServletResponse.SC_FORBIDDEN, "Tu rol no tiene permiso para realizar esta acción");
    }

    private void escribirError(HttpServletResponse response, int status, String mensaje) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("mensaje", mensaje);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
