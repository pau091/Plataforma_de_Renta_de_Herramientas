package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integración: autenticación JWT y restricciones de acceso por rol")
class SeguridadIT extends IntegracionBase {

    @Test
    @DisplayName("El login devuelve un JWT con el rol y el id del usuario")
    void loginDevuelveJwt() {
        JsonNode r = login(ADMIN, CLAVE);
        assertThat(r.get("token").asText().split("\\.")).hasSize(3);
        assertThat(r.get("rol").asText()).isEqualTo("ADMIN");
        assertThat(r.get("usuarioId").asLong()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Credenciales inválidas → 401 con el formato de error común")
    void credencialesInvalidas() throws Exception {
        JsonNode r = esperar(POST("/api/auth/login", null, Map.of("email", CLIENTE, "password", "ClaveIncorrecta1")), 401);
        assertThat(r.get("status").asInt()).isEqualTo(401);
        assertThat(r.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(r.get("message").asText()).isEqualTo("Credenciales inválidas.");
        assertThat(r.has("timestamp")).isTrue();
    }

    @Test
    @DisplayName("Un usuario inactivo no puede iniciar sesión → 403")
    void usuarioInactivo() throws Exception {
        JsonNode r = esperar(POST("/api/auth/login", null, Map.of("email", INACTIVO, "password", CLAVE)), 403);
        assertThat(r.get("message").asText()).contains("inactivo");
    }

    @Test
    @DisplayName("El registro público crea un CLIENTE cuyo token ya funciona; pedir PROVEEDOR → 403")
    void registroPublico() throws Exception {
        JsonNode r = esperar(POST("/api/auth/registro", null, Map.of("nombre", "Cliente IT",
                "email", "cliente.it." + System.nanoTime() + "@prueba.test", "password", CLAVE)), 201);
        assertThat(r.get("rol").asText()).isEqualTo("CLIENTE");
        mvc.perform(get("/api/panel").header(HttpHeaders.AUTHORIZATION, "Bearer " + r.get("token").asText()))
                .andExpect(status().isOk());

        esperar(POST("/api/auth/registro", null, Map.of("nombre", "Falso", "email", "falso." + System.nanoTime() + "@prueba.test",
                "password", CLAVE, "rol", "PROVEEDOR")), 403);
    }

    @Test
    @DisplayName("Datos inválidos → 400 con el error de cada campo")
    void registroInvalido() throws Exception {
        JsonNode r = esperar(POST("/api/auth/registro", null, Map.of("nombre", "", "email", "no-es-correo", "password", "123")), 400);
        assertThat(r.get("errores").propertyNames()).containsExactlyInAnyOrder("nombre", "email", "password");
    }

    @Test
    @DisplayName("Sin token o con un token alterado → 401")
    void sinTokenOTokenAlterado() throws Exception {
        JsonNode sinToken = esperar(get("/api/reservas"), 401);
        assertThat(sinToken.get("message").asText()).contains("Authorization: Bearer");

        String alterado = token(CLIENTE).substring(0, token(CLIENTE).length() - 4) + "abcd";
        JsonNode r = esperar(get("/api/reservas").header(HttpHeaders.AUTHORIZATION, "Bearer " + alterado), 401);
        assertThat(r.get("message").asText()).contains("token es inválido");
    }

    @Test
    @DisplayName("Al desactivar a un usuario, su token vigente deja de servir de inmediato")
    void tokenDeUsuarioDesactivado() throws Exception {
        String correo = "temporal." + System.nanoTime() + "@prueba.test";
        long id = esperar(POST("/api/usuarios", ADMIN, Map.of("nombre", "Temporal", "email", correo,
                "password", CLAVE, "rol", "CLIENTE")), 201).get("id").asLong();
        String tokenTemporal = login(correo, CLAVE).get("token").asText();
        mvc.perform(get("/api/panel").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTemporal)).andExpect(status().isOk());

        esperar(PUT("/api/usuarios/" + id, ADMIN, Map.of("activo", false)), 200);

        JsonNode r = esperar(get("/api/panel").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTemporal), 401);
        assertThat(r.get("message").asText()).contains("inactivo");
    }

    @ParameterizedTest(name = "{0} {1} como {2} → {3}")
    @CsvSource({
            "GET,    /api/reportes/ingresos,      juan.cliente@gmail.com,     403",
            "GET,    /api/reportes/ingresos,      carlos.proveedor@gmail.com, 403",
            "GET,    /api/reportes/ingresos,      admin@rentas.com,           200",
            "GET,    /api/usuarios,               carlos.proveedor@gmail.com, 403",
            "GET,    /api/usuarios,               admin@rentas.com,           200",
            "GET,    /api/mantenimientos,         juan.cliente@gmail.com,     403",
            "GET,    /api/mantenimientos,         carlos.proveedor@gmail.com, 200",
            "POST,   /api/herramientas,           juan.cliente@gmail.com,     403",
            "POST,   /api/categorias,             carlos.proveedor@gmail.com, 403",
            "POST,   /api/pagos,                  carlos.proveedor@gmail.com, 403",
            "PUT,    /api/pagos/1,                juan.cliente@gmail.com,     403",
            "DELETE, /api/pagos/1,                carlos.proveedor@gmail.com, 403",
            "POST,   /api/reservas/vencer,        carlos.proveedor@gmail.com, 403",
            "GET,    /api/reservas/2,             juan.cliente@gmail.com,     403",
            "GET,    /api/herramientas,           juan.cliente@gmail.com,     200",
            "GET,    /api/panel,                  carlos.proveedor@gmail.com, 200",
    })
    void permisosPorRol(String metodo, String url, String usuario, int esperado) throws Exception {
        var peticion = switch (metodo) {
            case "GET" -> GET(url, usuario);
            case "POST" -> POST(url, usuario, Map.of());
            case "PUT" -> PUT(url, usuario, Map.of("metodoPago", "EFECTIVO"));
            case "DELETE" -> DELETE(url, usuario);
            default -> throw new IllegalArgumentException(metodo);
        };
        assertThat(mvc.perform(peticion).andReturn().getResponse().getStatus()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Frontend, imágenes y Swagger son públicos; la API no")
    void recursosPublicos() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api/herramientas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS permite al frontend local (cualquier puerto de localhost)")
    void cors() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
        mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, "https://sitio-malicioso.com"))
                .andExpect(status().isForbidden());
    }
}
