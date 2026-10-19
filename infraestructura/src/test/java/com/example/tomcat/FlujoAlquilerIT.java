package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integración: flujos completos de alquiler, pago, devolución, cancelación y reembolso")
class FlujoAlquilerIT extends IntegracionBase {

    @Autowired
    JdbcTemplate jdbc;

    private JsonNode reserva(long id, String usuario) throws Exception {
        return esperar(GET("/api/reservas/" + id, usuario), 200);
    }

    private static boolean contieneId(JsonNode lista, long id, String campo) {
        return StreamSupport.stream(lista.spliterator(), false).anyMatch(n -> n.get(campo).asLong() == id);
    }

    @Test
    @DisplayName("Reservar → aceptar → pagar (confirmación + factura) → entregar → devolver con daño → reparar")
    void flujoCompleto() throws Exception {
        long herramienta = nuevaHerramienta("Cortadora IT", 30000);
        long id = reservar(CLIENTE, herramienta, dia(10), dia(12));
        assertThat(reserva(id, CLIENTE).get("estado").asText()).isEqualTo("PENDIENTE");

        // El proveedor ve la solicitud; otro proveedor no puede aceptarla.
        assertThat(contieneId(esperar(GET("/api/reservas?estado=PENDIENTE", PROVEEDOR), 200), id, "id")).isTrue();
        esperar(POST("/api/reservas/" + id + "/aceptar", OTRO_PROVEEDOR, null), 403);
        assertThat(esperar(POST("/api/reservas/" + id + "/aceptar", PROVEEDOR, null), 200).get("estado").asText()).isEqualTo("ACEPTADA");

        // Pago: el monto debe ser tarifa × días (3 × 30.000).
        JsonNode error = esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 1000, "metodoPago", "TARJETA")), 400);
        assertThat(error.get("message").asText()).contains("90000");
        JsonNode pago = esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 90000, "metodoPago", "TARJETA")), 201);
        assertThat(pago.get("estadoPago").asText()).isEqualTo("COMPLETADO");
        assertThat(reserva(id, CLIENTE).get("estado").asText()).isEqualTo("CONFIRMADA");
        esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 90000, "metodoPago", "TARJETA")), 409);

        // Factura automática y su descarga.
        long pagoId = pago.get("id").asLong();
        JsonNode factura = esperar(GET("/api/facturas/pago/" + pagoId, CLIENTE), 200);
        assertThat(factura.get("numero").asText()).isEqualTo(String.format("FAC-%06d", pagoId));
        assertThat(factura.get("total").decimalValue()).isEqualByComparingTo("90000");
        assertThat(factura.get("dias").asInt()).isEqualTo(3);
        assertThat(factura.get("estado").asText()).isEqualTo("VIGENTE");
        MvcResult descarga = mvc.perform(GET("/api/facturas/" + factura.get("id").asLong() + "/descargar", CLIENTE)).andReturn();
        assertThat(descarga.getResponse().getStatus()).isEqualTo(200);
        assertThat(descarga.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains(factura.get("numero").asText() + ".html");
        assertThat(descarga.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("Comprobante de pago");
        esperar(GET("/api/facturas/" + factura.get("id").asLong(), OTRO_CLIENTE), 403);

        // Entrega y devolución con daño.
        assertThat(esperar(POST("/api/reservas/" + id + "/entregar", PROVEEDOR, null), 200).get("estado").asText()).isEqualTo("ENTREGADA");
        JsonNode devuelta = esperar(POST("/api/reservas/" + id + "/devolver", PROVEEDOR,
                Map.of("estadoEquipo", "DAÑADO", "observaciones", "Disco partido")), 200);
        assertThat(devuelta.get("estado").asText()).isEqualTo("FINALIZADA");
        assertThat(devuelta.get("estadoDevolucion").asText()).isEqualTo("DANADO");
        assertThat(esperar(GET("/api/herramientas/" + herramienta, CLIENTE), 200).get("estado").asText()).isEqualTo("MANTENIMIENTO");

        // Incidencia y reparación.
        JsonNode mantenimientos = esperar(GET("/api/mantenimientos?reservaId=" + id, PROVEEDOR), 200);
        assertThat(mantenimientos).hasSize(1);
        assertThat(mantenimientos.get(0).get("fechaFin").isNull()).isTrue();
        assertThat(contieneId(esperar(GET("/api/reportes/incidencias", ADMIN), 200).get("items"), id, "reservaId")).isTrue();
        esperar(POST("/api/mantenimientos/" + mantenimientos.get(0).get("id").asLong() + "/cerrar", PROVEEDOR,
                Map.of("costoReparacion", 20000)), 200);
        assertThat(esperar(GET("/api/herramientas/" + herramienta, CLIENTE), 200).get("estado").asText()).isEqualTo("DISPONIBLE");

        // Rentabilidad de la herramienta: 90.000 - 20.000.
        JsonNode item = StreamSupport.stream(esperar(GET("/api/reportes/rentabilidad", ADMIN), 200).get("items").spliterator(), false)
                .filter(n -> n.get("herramientaId").asLong() == herramienta).findFirst().orElseThrow();
        assertThat(item.get("utilidad").decimalValue()).isEqualByComparingTo("70000");

        // Lo que tiene historial no se borra.
        esperar(DELETE("/api/herramientas/" + herramienta, PROVEEDOR), 409);
    }

    @Test
    @DisplayName("Validación de disponibilidad y de datos antes de reservar")
    void validacionesAlReservar() throws Exception {
        long herramienta = nuevaHerramienta("Andamio IT", 20000);
        reservar(CLIENTE, herramienta, dia(5), dia(7));

        JsonNode cruce = esperar(POST("/api/reservas", OTRO_CLIENTE, Map.of("herramientaId", herramienta,
                "fechaInicio", dia(7), "fechaFin", dia(8))), 409);
        assertThat(cruce.get("message").asText()).contains("ya tiene una reserva en esas fechas");
        esperar(POST("/api/reservas", CLIENTE, Map.of("herramientaId", herramienta, "fechaInicio", dia(-1), "fechaFin", dia(1))), 400);
        JsonNode sinFechas = esperar(POST("/api/reservas", CLIENTE, Map.of("herramientaId", herramienta)), 400);
        assertThat(sinFechas.get("errores").has("fechaInicio")).isTrue();
        esperar(POST("/api/reservas", CLIENTE, Map.of("herramientaId", 999999, "fechaInicio", dia(3), "fechaFin", dia(3))), 404);
        esperar(POST("/api/reservas", PROVEEDOR, Map.of("herramientaId", herramienta, "fechaInicio", dia(20), "fechaFin", dia(20))), 403);

        JsonNode libre = esperar(GET("/api/reservas/disponibilidad/" + herramienta + "?fechaInicio=" + dia(6) + "&fechaFin=" + dia(6), CLIENTE), 200);
        assertThat(libre.get("disponible").asBoolean()).isFalse();
        libre = esperar(GET("/api/reservas/disponibilidad/" + herramienta + "?fechaInicio=" + dia(9) + "&fechaFin=" + dia(9), CLIENTE), 200);
        assertThat(libre.get("disponible").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("El cliente cambia las fechas (PUT) y cancela (DELETE): la reserva queda en el historial y libera las fechas")
    void modificarYCancelar() throws Exception {
        long herramienta = nuevaHerramienta("Pulidora IT", 15000);
        long id = reservar(CLIENTE, herramienta, dia(5), dia(6));

        JsonNode modificada = esperar(PUT("/api/reservas/" + id, CLIENTE, Map.of("fechaInicio", dia(8), "fechaFin", dia(9))), 200);
        assertThat(modificada.get("fechaInicio").asText()).isEqualTo(dia(8));
        esperar(PUT("/api/reservas/" + id, OTRO_CLIENTE, Map.of("fechaInicio", dia(8), "fechaFin", dia(9))), 403);
        esperar(DELETE("/api/reservas/" + id, PROVEEDOR), 403);

        assertThat(esperar(DELETE("/api/reservas/" + id, CLIENTE), 200).get("estado").asText()).isEqualTo("CANCELADA");
        assertThat(reserva(id, CLIENTE).get("estado").asText()).isEqualTo("CANCELADA");
        esperar(DELETE("/api/reservas/" + id, CLIENTE), 400);
        reservar(OTRO_CLIENTE, herramienta, dia(8), dia(9)); // las fechas quedaron libres
    }

    @Test
    @DisplayName("Reembolso (ADMIN): pago REEMBOLSADO, reserva CANCELADA, factura ANULADA y deja de contar como ingreso")
    void reembolso() throws Exception {
        long herramienta = nuevaHerramienta("Compresor IT", 40000);
        long id = reservar(CLIENTE, herramienta, dia(15), dia(16));
        esperar(POST("/api/reservas/" + id + "/aceptar", PROVEEDOR, null), 200);
        long pago = esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 80000, "metodoPago", "EFECTIVO")), 201)
                .get("id").asLong();
        var ingresosAntes = esperar(GET("/api/reportes/ingresos", ADMIN), 200).get("totalIngresos").decimalValue();

        JsonNode cancelar = esperar(DELETE("/api/reservas/" + id, CLIENTE), 400);
        assertThat(cancelar.get("message").asText()).contains("reembolsar");
        esperar(DELETE("/api/pagos/" + pago, CLIENTE), 403);
        assertThat(esperar(PUT("/api/pagos/" + pago, ADMIN, Map.of("metodoPago", "transferencia")), 200)
                .get("metodoPago").asText()).isEqualTo("TRANSFERENCIA");

        assertThat(esperar(DELETE("/api/pagos/" + pago, ADMIN), 200).get("estadoPago").asText()).isEqualTo("REEMBOLSADO");
        assertThat(reserva(id, CLIENTE).get("estado").asText()).isEqualTo("CANCELADA");
        JsonNode factura = esperar(GET("/api/facturas/pago/" + pago, CLIENTE), 200);
        assertThat(factura.get("estado").asText()).isEqualTo("ANULADA");
        assertThat(factura.get("metodoPago").asText()).isEqualTo("TRANSFERENCIA");
        var ingresosDespues = esperar(GET("/api/reportes/ingresos", ADMIN), 200).get("totalIngresos").decimalValue();
        assertThat(ingresosAntes.subtract(ingresosDespues)).isEqualByComparingTo("80000");
        esperar(DELETE("/api/pagos/" + pago, ADMIN), 400);
    }

    @Test
    @DisplayName("Vencimiento: pasada 1 hora no se puede aceptar y el proceso la marca VENCIDA")
    void vencimientoPorPlazo() throws Exception {
        long herramienta = nuevaHerramienta("Vibrador IT", 25000);
        long id = reservar(CLIENTE, herramienta, dia(30), dia(31));
        // Simula que la solicitud se creó hace 61 minutos.
        jdbc.update("UPDATE reservas SET created_at = created_at - INTERVAL 61 MINUTE WHERE id = ?", id);

        JsonNode error = esperar(POST("/api/reservas/" + id + "/aceptar", PROVEEDOR, null), 409);
        assertThat(error.get("message").asText()).contains("venció");

        JsonNode vencidas = esperar(POST("/api/reservas/vencer", ADMIN, null), 200);
        assertThat(contieneId(vencidas, id, "id")).isTrue();
        assertThat(reserva(id, CLIENTE).get("estado").asText()).isEqualTo("VENCIDA");
        reservar(OTRO_CLIENTE, herramienta, dia(30), dia(31)); // una reserva vencida libera las fechas
    }
}
