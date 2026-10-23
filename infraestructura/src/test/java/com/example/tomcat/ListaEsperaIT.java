package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integración: lista de espera y aviso al primero de la fila cuando se liberan las fechas")
class ListaEsperaIT extends IntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    private JsonNode entrada(String usuario, long herramienta) throws Exception {
        return StreamSupport.stream(esperar(GET("/api/lista-espera/mias", usuario), 200).spliterator(), false)
                .filter(e -> e.get("herramientaId").asLong() == herramienta).findFirst().orElseThrow();
    }

    private boolean tieneAviso(String usuario, long herramienta) throws Exception {
        return StreamSupport.stream(esperar(GET("/api/notificaciones", usuario), 200).spliterator(), false)
                .anyMatch(n -> n.get("herramientaId").asLong() == herramienta && !n.get("leida").asBoolean());
    }

    @Test
    @DisplayName("Si el cliente cancela, se avisa al primero de la fila; al reservar, su entrada queda ATENDIDA")
    void cancelacionAvisaAlPrimero() throws Exception {
        long herramienta = nuevaHerramienta("Mezcladora espera", 30000);
        long ocupada = reservar(CLIENTE, herramienta, dia(10), dia(12));

        // Las fechas están ocupadas: Ana se anota primero y María (proveedora que alquila) después.
        esperar(POST("/api/lista-espera", OTRO_CLIENTE, Map.of("herramientaId", herramienta, "fechaInicio", dia(11), "fechaFin", dia(13))), 201);
        esperar(POST("/api/lista-espera", OTRO_CLIENTE, Map.of("herramientaId", herramienta, "fechaInicio", dia(11), "fechaFin", dia(12))), 409);
        esperar(POST("/api/lista-espera", OTRO_PROVEEDOR, Map.of("herramientaId", herramienta, "fechaInicio", dia(10), "fechaFin", dia(12))), 201);
        JsonNode libre = esperar(POST("/api/lista-espera", OTRO_CLIENTE,
                Map.of("herramientaId", herramienta, "fechaInicio", dia(20), "fechaFin", dia(21))), 400);
        assertThat(libre.get("message").asText()).contains("reservarla directamente");

        esperar(DELETE("/api/reservas/" + ocupada, CLIENTE), 200);

        assertThat(entrada(OTRO_CLIENTE, herramienta).get("estado").asText()).isEqualTo("NOTIFICADA");
        assertThat(tieneAviso(OTRO_CLIENTE, herramienta)).isTrue();
        assertThat(entrada(OTRO_PROVEEDOR, herramienta).get("estado").asText()).isEqualTo("ESPERANDO");   // solo al primero
        assertThat(esperar(GET("/api/notificaciones/no-leidas", OTRO_CLIENTE), 200).get("total").asLong()).isPositive();

        reservar(OTRO_CLIENTE, herramienta, dia(11), dia(13));
        assertThat(entrada(OTRO_CLIENTE, herramienta).get("estado").asText()).isEqualTo("ATENDIDA");
    }

    @Test
    @DisplayName("Si la reserva vence sin respuesta del proveedor, también se avisa al que espera")
    void vencimientoAvisa() throws Exception {
        long herramienta = nuevaHerramienta("Taladro vence", 25000);
        long ocupada = reservar(CLIENTE, herramienta, dia(15), dia(16));
        esperar(POST("/api/lista-espera", OTRO_CLIENTE, Map.of("herramientaId", herramienta, "fechaInicio", dia(15), "fechaFin", dia(16))), 201);

        // El proveedor no respondió en 1 hora.
        jdbc.update("UPDATE reservas SET created_at = created_at - INTERVAL 2 HOUR WHERE id = ?", ocupada);
        esperar(POST("/api/reservas/vencer", ADMIN, null), 200);

        assertThat(entrada(OTRO_CLIENTE, herramienta).get("estado").asText()).isEqualTo("NOTIFICADA");
        assertThat(tieneAviso(OTRO_CLIENTE, herramienta)).isTrue();
    }

    @Test
    @DisplayName("Cada uno solo ve y marca sus notificaciones; puede salir de la lista")
    void notificacionesYCancelar() throws Exception {
        long herramienta = nuevaHerramienta("Compresor espera", 20000);
        long ocupada = reservar(CLIENTE, herramienta, dia(25), dia(26));
        long id = esperar(POST("/api/lista-espera", OTRO_CLIENTE,
                Map.of("herramientaId", herramienta, "fechaInicio", dia(25), "fechaFin", dia(26))), 201).get("id").asLong();
        esperar(POST("/api/reservas/" + ocupada + "/rechazar", PROVEEDOR, null), 200);   // rechazar también libera

        long aviso = StreamSupport.stream(esperar(GET("/api/notificaciones", OTRO_CLIENTE), 200).spliterator(), false)
                .filter(n -> n.get("herramientaId").asLong() == herramienta).findFirst().orElseThrow().get("id").asLong();
        esperar(PATCH("/api/notificaciones/" + aviso + "/leida", CLIENTE, Map.of()), 403);
        assertThat(esperar(PATCH("/api/notificaciones/" + aviso + "/leida", OTRO_CLIENTE, Map.of()), 200)
                .get("leida").asBoolean()).isTrue();
        esperar(POST("/api/notificaciones/leidas", OTRO_CLIENTE, null), 204);
        assertThat(esperar(GET("/api/notificaciones/no-leidas", OTRO_CLIENTE), 200).get("total").asLong()).isZero();

        esperar(DELETE("/api/lista-espera/" + id, CLIENTE), 403);
        assertThat(esperar(DELETE("/api/lista-espera/" + id, OTRO_CLIENTE), 200).get("estado").asText()).isEqualTo("CANCELADA");
    }
}
