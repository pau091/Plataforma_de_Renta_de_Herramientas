package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integración: calificaciones de 1 a 5 estrellas al terminar un alquiler")
class CalificacionesIT extends IntegracionBase {

    /** Alquiler completo hasta FINALIZADA: reservar, aceptar, pagar, entregar y devolver en buen estado. */
    private long alquilerTerminado(long herramienta) throws Exception {
        long id = reservar(CLIENTE, herramienta, dia(2), dia(3));
        esperar(POST("/api/reservas/" + id + "/aceptar", PROVEEDOR, null), 200);
        esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 20000, "metodoPago", "TARJETA")), 201);
        esperar(POST("/api/reservas/" + id + "/entregar", PROVEEDOR, null), 200);
        esperar(POST("/api/reservas/" + id + "/devolver", PROVEEDOR, Map.of("estadoEquipo", "BUENO")), 200);
        return id;
    }

    @Test
    @DisplayName("Solo quien alquiló califica, una vez, con 1 a 5 estrellas; el catálogo ve el promedio")
    void calificar() throws Exception {
        long herramienta = nuevaHerramienta("Lijadora resenas", 10000);
        long id = alquilerTerminado(herramienta);

        esperar(POST("/api/reservas/" + id + "/calificacion", OTRO_CLIENTE, Map.of("estrellasHerramienta", 1)), 403);
        JsonNode invalida = esperar(POST("/api/reservas/" + id + "/calificacion", CLIENTE, Map.of("estrellasHerramienta", 6)), 400);
        assertThat(invalida.get("errores").has("estrellasHerramienta")).isTrue();

        JsonNode resena = esperar(POST("/api/reservas/" + id + "/calificacion", CLIENTE, Map.of(
                "estrellasHerramienta", 5, "estrellasProveedor", 4, "comentario", "Funcionó perfecto")), 201);
        assertThat(resena.get("clienteNombre").asText()).isEqualTo("Juan Cliente");
        esperar(POST("/api/reservas/" + id + "/calificacion", CLIENTE, Map.of("estrellasHerramienta", 3)), 409);

        JsonNode resenas = esperar(GET("/api/herramientas/" + herramienta + "/calificaciones", OTRO_CLIENTE), 200);
        assertThat(resenas).hasSize(1);
        assertThat(resenas.get(0).get("comentario").asText()).isEqualTo("Funcionó perfecto");

        JsonNode promedio = StreamSupport.stream(esperar(GET("/api/calificaciones/resumen", CLIENTE), 200)
                        .get("herramientas").spliterator(), false)
                .filter(p -> p.get("id").asLong() == herramienta).findFirst().orElseThrow();
        assertThat(promedio.get("promedio").decimalValue()).isEqualByComparingTo("5.0");
        assertThat(promedio.get("total").asLong()).isEqualTo(1);
        assertThat(StreamSupport.stream(esperar(GET("/api/calificaciones/mias", CLIENTE), 200).spliterator(), false)
                .anyMatch(c -> c.get("reservaId").asLong() == id)).isTrue();
    }

    @Test
    @DisplayName("Un alquiler que no ha terminado no se puede calificar; los datos de demostración traen reseñas")
    void soloTerminadosYDemo() throws Exception {
        long id = reservar(CLIENTE, nuevaHerramienta("Escalera sin terminar", 10000), dia(4), dia(4));
        JsonNode error = esperar(POST("/api/reservas/" + id + "/calificacion", CLIENTE, Map.of("estrellasHerramienta", 5)), 400);
        assertThat(error.get("message").asText()).contains("FINALIZADA");

        assertThat(esperar(GET("/api/calificaciones/resumen", CLIENTE), 200).get("herramientas").size()).isGreaterThan(5);
        // La reserva 32 de la demostración (de juan.cliente) quedó sin calificar a propósito.
        esperar(POST("/api/reservas/32/calificacion", CLIENTE, Map.of("estrellasHerramienta", 4, "estrellasProveedor", 5)), 201);
    }
}
