package com.example.tomcat.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final JsonSecurityErrorHandler jsonSecurityErrorHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource,
            JsonSecurityErrorHandler jsonSecurityErrorHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.jsonSecurityErrorHandler = jsonSecurityErrorHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jsonSecurityErrorHandler)
                .accessDeniedHandler(jsonSecurityErrorHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Frontend estático (HTML, CSS, JS e imágenes): público; los datos se piden a la API con el token.
                .requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/herramientas/**").hasAnyRole("ADMIN", "PROVEEDOR")
                .requestMatchers(HttpMethod.PUT, "/api/herramientas/**").hasAnyRole("ADMIN", "PROVEEDOR")
                .requestMatchers(HttpMethod.PATCH, "/api/herramientas/**").hasAnyRole("ADMIN", "PROVEEDOR")
                .requestMatchers(HttpMethod.DELETE, "/api/herramientas/**").hasAnyRole("ADMIN", "PROVEEDOR")
                .requestMatchers(HttpMethod.GET, "/api/herramientas/**").hasAnyRole("ADMIN", "PROVEEDOR", "CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").hasAnyRole("ADMIN", "PROVEEDOR", "CLIENTE")
                .requestMatchers("/api/categorias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/reservas/*/aceptar", "/api/reservas/*/rechazar",
                                 "/api/reservas/*/entregar", "/api/reservas/*/devolver")
                    .hasAnyRole("ADMIN", "PROVEEDOR")
                .requestMatchers(HttpMethod.POST, "/api/reservas/vencer").hasRole("ADMIN")
                // El proveedor también puede alquilar equipos de otros proveedores (el servicio valida la titularidad).
                .requestMatchers(HttpMethod.POST, "/api/reservas").hasAnyRole("ADMIN", "CLIENTE", "PROVEEDOR")
                .requestMatchers(HttpMethod.PUT, "/api/reservas/*").hasAnyRole("ADMIN", "CLIENTE", "PROVEEDOR")
                .requestMatchers(HttpMethod.DELETE, "/api/reservas/*").hasAnyRole("ADMIN", "CLIENTE", "PROVEEDOR")
                .requestMatchers("/api/reservas/**").hasAnyRole("ADMIN", "PROVEEDOR", "CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/pagos").hasAnyRole("ADMIN", "CLIENTE", "PROVEEDOR")
                .requestMatchers(HttpMethod.PUT, "/api/pagos/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/pagos/*").hasRole("ADMIN")
                .requestMatchers("/api/pagos/**").hasAnyRole("ADMIN", "PROVEEDOR", "CLIENTE")
                .requestMatchers("/api/facturas/**").hasAnyRole("ADMIN", "PROVEEDOR", "CLIENTE")
                .requestMatchers("/api/reportes/**").hasRole("ADMIN")
                .requestMatchers("/api/mantenimientos/**").hasAnyRole("ADMIN", "PROVEEDOR")
                .requestMatchers("/api/panel").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
