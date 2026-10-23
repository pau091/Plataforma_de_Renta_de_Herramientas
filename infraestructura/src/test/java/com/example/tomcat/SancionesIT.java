package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Integración: suspensión y baneo con expulsión inmediata (lista negra de tokens)")
class SancionesIT extends IntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    private long nuevoCliente(String correo) throws Exception {
        return esperar(POST("/api/usuarios", ADMIN, Map.of("nombre", "Cliente Conflictivo", "email", correo,
                "password", CLAVE, "rol", "CLIENTE")), 201).get("id").asLong();
    }

    /** Petición con un token concreto (sin la caché de tokens de IntegracionBase). */
    private int panelCon(String token) throws Exception {
        return mvc.perform(get("/api/panel").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode panelRechazado(String token) throws Exception {
        return esperar(get("/api/panel").header(HttpHeaders.AUTHORIZATION, "Bearer " + token), 401);
    }

    @Test
    @DisplayName("Suspender expulsa al instante todas sus sesiones; no puede entrar hasta que se levante la sanción")
    void suspension() throws Exception {
        String correo = "suspendido." + System.nanoTime() + "@prueba.test";
        long id = nuevoCliente(correo);
        String sesion1 = login(correo, CLAVE).get("token").asText();
        String sesion2 = login(correo, CLAVE).get("token").asText();
        assertThat(panelCon(sesion1)).isEqualTo(200);
        assertThat(panelCon(sesion2)).isEqualTo(200);

        JsonNode sancion = esperar(POST("/api/usuarios/" + id + "/suspension", ADMIN,
                Map.of("dias", 3, "motivo", "Cancelaciones reiteradas")), 200);
        assertThat(sancion.get("tokensRevocados").asInt()).isEqualTo(2);
        assertThat(sancion.get("usuario").get("estadoCuenta").asText()).isEqualTo("SUSPENDIDA");
        assertThat(sancion.get("usuario").get("motivoSancion").asText()).isEqualTo("Cancelaciones reiteradas");

        // Los dos tokens siguen firmados y vigentes, pero están en la lista negra.
        assertThat(panelRechazado(sesion1).get("message").asText())
                .contains("revocada").contains("Cuenta suspendida: Cancelaciones reiteradas");
        assertThat(panelCon(sesion2)).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tokens_revocados WHERE usuario_id = ?", Integer.class, id))
                .isEqualTo(2);

        JsonNode login = esperar(POST("/api/auth/login", null, Map.of("email", correo, "password", CLAVE)), 403);
        assertThat(login.get("message").asText()).contains("suspendida hasta").contains("Cancelaciones reiteradas");

        // Al reactivar puede volver a entrar, pero los tokens revocados no resucitan.
        assertThat(esperar(POST("/api/usuarios/" + id + "/reactivacion", ADMIN, null), 200)
                .get("estadoCuenta").asText()).isEqualTo("ACTIVA");
        assertThat(panelCon(sesion1)).isEqualTo(401);
        assertThat(panelCon(login(correo, CLAVE).get("token").asText())).isEqualTo(200);
    }

    @Test
    @DisplayName("Banear también revoca sus tokens y el login queda bloqueado indefinidamente")
    void baneo() throws Exception {
        String correo = "baneado." + System.nanoTime() + "@prueba.test";
        long id = nuevoCliente(correo);
        String sesion = login(correo, CLAVE).get("token").asText();

        assertThat(esperar(POST("/api/usuarios/" + id + "/baneo", ADMIN, Map.of("motivo", "Fraude en pagos")), 200)
                .get("tokensRevocados").asInt()).isEqualTo(1);
        assertThat(panelRechazado(sesion).get("message").asText()).contains("Cuenta baneada: Fraude en pagos");
        assertThat(esperar(POST("/api/auth/login", null, Map.of("email", correo, "password", CLAVE)), 403)
                .get("message").asText()).contains("baneada");
        assertThat(esperar(GET("/api/usuarios/" + id, ADMIN), 200).get("estadoCuenta").asText()).isEqualTo("BANEADA");
    }

    @Test
    @DisplayName("Cerrar sesión revoca solo ese token; las otras sesiones siguen activas")
    void cerrarSesion() throws Exception {
        String correo = "logout." + System.nanoTime() + "@prueba.test";
        nuevoCliente(correo);
        String sesion1 = login(correo, CLAVE).get("token").asText();
        String sesion2 = login(correo, CLAVE).get("token").asText();

        assertThat(mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion1))
                .andReturn().getResponse().getStatus()).isEqualTo(204);
        assertThat(panelRechazado(sesion1).get("message").asText()).contains("Cierre de sesión");
        assertThat(panelCon(sesion2)).isEqualTo(200);
        esperar(post("/api/auth/logout"), 401);   // sin token
    }

    @Test
    @DisplayName("Solo el ADMIN sanciona, solo a proveedores y clientes, con días y motivo válidos")
    void reglas() throws Exception {
        long id = nuevoCliente("reglas." + System.nanoTime() + "@prueba.test");
        esperar(POST("/api/usuarios/" + id + "/baneo", CLIENTE, Map.of("motivo", "x")), 403);
        esperar(POST("/api/usuarios/" + id + "/suspension", PROVEEDOR, Map.of("dias", 1, "motivo", "x")), 403);
        esperar(POST("/api/usuarios/1/baneo", ADMIN, Map.of("motivo", "x")), 400);
        JsonNode error = esperar(POST("/api/usuarios/" + id + "/suspension", ADMIN, Map.of("dias", 0, "motivo", "")), 400);
        assertThat(error.get("errores").has("dias")).isTrue();
        assertThat(error.get("errores").has("motivo")).isTrue();
        esperar(POST("/api/usuarios/" + id + "/reactivacion", ADMIN, null), 400);   // no estaba sancionado
        esperar(POST("/api/usuarios/999999/baneo", ADMIN, Map.of("motivo", "x")), 404);
    }
}
