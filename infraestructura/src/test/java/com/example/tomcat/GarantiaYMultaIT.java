package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integración: depósito de garantía, multa por devolución tardía y su liquidación")
class GarantiaYMultaIT extends IntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    /** Herramienta del PROVEEDOR con tarifa 20.000 y depósito 50.000. */
    private long herramientaConDeposito(String nombre) throws Exception {
        return esperar(POST("/api/herramientas", PROVEEDOR, Map.of("categoriaId", 2, "nombre", nombre + " " + System.nanoTime(),
                "descripcion", "Con depósito de garantía", "tarifaDiaria", 20000, "deposito", 50000)), 201).get("id").asLong();
    }

    /** Reserva de 2 días aceptada, pagada (40.000 + 50.000) y entregada. */
    private long alquilerEntregado(long herramienta, int desde) throws Exception {
        long id = reservar(CLIENTE, herramienta, dia(desde), dia(desde + 1));
        esperar(POST("/api/reservas/" + id + "/aceptar", PROVEEDOR, null), 200);
        esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 90000, "metodoPago", "TARJETA")), 201);
        esperar(POST("/api/reservas/" + id + "/entregar", PROVEEDOR, null), 200);
        return id;
    }

    private JsonNode factura(long reservaId) throws Exception {
        return esperar(GET("/api/facturas/reserva/" + reservaId, CLIENTE), 200);
    }

    @Test
    @DisplayName("Se paga alquiler + depósito; la factura los separa y el depósito queda RETENIDO")
    void pagoConDeposito() throws Exception {
        long herramienta = herramientaConDeposito("Cortadora deposito");
        assertThat(esperar(GET("/api/herramientas/" + herramienta, CLIENTE), 200).get("deposito").decimalValue())
                .isEqualByComparingTo("50000");
        long id = reservar(CLIENTE, herramienta, dia(2), dia(3));
        esperar(POST("/api/reservas/" + id + "/aceptar", PROVEEDOR, null), 200);

        JsonNode error = esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 40000, "metodoPago", "TARJETA")), 400);
        assertThat(error.get("message").asText()).contains("90000").contains("depósito de garantía de 50000");
        esperar(POST("/api/pagos", CLIENTE, Map.of("reservaId", id, "monto", 90000, "metodoPago", "TARJETA")), 201);

        JsonNode garantia = esperar(GET("/api/reservas/" + id, CLIENTE), 200).get("garantia");
        assertThat(garantia.get("estado").asText()).isEqualTo("RETENIDO");
        assertThat(garantia.get("deposito").decimalValue()).isEqualByComparingTo("50000");
        JsonNode f = factura(id);
        assertThat(f.get("subtotal").decimalValue()).isEqualByComparingTo("40000");
        assertThat(f.get("total").decimalValue()).isEqualByComparingTo("90000");
    }

    @Test
    @DisplayName("Devolución 1 día tarde: multa 30.000 (20.000 × 1,5) descontada del depósito, en la factura y en los reportes")
    void devolucionTardia() throws Exception {
        long id = alquilerEntregado(herramientaConDeposito("Taladro tarde"), 1);
        // Simula que la fecha fin ya pasó: debía devolverse ayer.
        jdbc.update("UPDATE reservas SET fecha_inicio = ?, fecha_fin = ? WHERE id = ?", dia(-2), dia(-1), id);

        JsonNode garantia = esperar(POST("/api/reservas/" + id + "/devolver", PROVEEDOR, Map.of("estadoEquipo", "BUENO")), 200)
                .get("garantia");
        assertThat(garantia.get("diasRetraso").asInt()).isEqualTo(1);
        assertThat(garantia.get("multaRetraso").decimalValue()).isEqualByComparingTo("30000");
        assertThat(garantia.get("montoDevuelto").decimalValue()).isEqualByComparingTo("20000");
        assertThat(garantia.get("estado").asText()).isEqualTo("DEVUELTO_PARCIAL");

        JsonNode f = factura(id);
        assertThat(f.get("garantia").get("multaRetraso").decimalValue()).isEqualByComparingTo("30000");
        byte[] pdf = mvc.perform(GET("/api/facturas/" + f.get("id").asLong() + "/descargar", CLIENTE))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(textoDelPdf(pdf)).contains("Liquidación del depósito", "1 día de retraso", "Multa por retraso",
                "$ 30.000 COP", "Depósito devuelto", "$ 20.000 COP");

        JsonNode multas = esperar(GET("/api/reportes/multas", ADMIN), 200);
        assertThat(StreamSupport.stream(multas.get("items").spliterator(), false)
                .anyMatch(m -> m.get("reservaId").asLong() == id && m.get("multa").decimalValue().intValue() == 30000)).isTrue();
        assertThat(esperar(GET("/api/reportes/ingresos", ADMIN), 200).get("ingresosMultas").decimalValue())
                .isGreaterThanOrEqualTo(new java.math.BigDecimal("30000"));
    }

    @Test
    @DisplayName("Dañada: el depósito espera el costo real; al cerrar la reparación (15.000) se devuelven 35.000")
    void devolucionConDano() throws Exception {
        long id = alquilerEntregado(herramientaConDeposito("Pulidora danada"), 3);
        JsonNode garantia = esperar(POST("/api/reservas/" + id + "/devolver", PROVEEDOR,
                Map.of("estadoEquipo", "DANADO", "observaciones", "Disco partido")), 200).get("garantia");
        assertThat(garantia.get("estado").asText()).isEqualTo("PENDIENTE_REPARACION");

        long mantenimiento = esperar(GET("/api/mantenimientos?reservaId=" + id, PROVEEDOR), 200).get(0).get("id").asLong();
        esperar(POST("/api/mantenimientos/" + mantenimiento + "/cerrar", PROVEEDOR, Map.of("costoReparacion", 15000)), 200);

        garantia = esperar(GET("/api/reservas/" + id, CLIENTE), 200).get("garantia");
        assertThat(garantia.get("cargoDanos").decimalValue()).isEqualByComparingTo("15000");
        assertThat(garantia.get("montoDevuelto").decimalValue()).isEqualByComparingTo("35000");
        assertThat(factura(id).get("garantia").get("estado").asText()).isEqualTo("DEVUELTO_PARCIAL");
    }

    @Test
    @DisplayName("Perdida: el depósito no se devuelve; a tiempo y en buen estado: se devuelve completo")
    void perdidaYBuenEstado() throws Exception {
        long perdida = alquilerEntregado(herramientaConDeposito("Nivel perdido"), 5);
        JsonNode g = esperar(POST("/api/reservas/" + perdida + "/devolver", PROVEEDOR, Map.of("estadoEquipo", "PERDIDO")), 200)
                .get("garantia");
        assertThat(g.get("estado").asText()).isEqualTo("NO_DEVUELTO");
        assertThat(g.get("montoDevuelto").decimalValue()).isEqualByComparingTo("0");

        long bien = alquilerEntregado(herramientaConDeposito("Escalera bien"), 7);
        g = esperar(POST("/api/reservas/" + bien + "/devolver", PROVEEDOR, Map.of("estadoEquipo", "BUENO")), 200).get("garantia");
        assertThat(g.get("estado").asText()).isEqualTo("DEVUELTO");
        assertThat(g.get("montoDevuelto").decimalValue()).isEqualByComparingTo("50000");
        assertThat(g.get("diasRetraso").asInt()).isZero();
    }

    @Test
    @DisplayName("El depósito no puede ser negativo")
    void depositoNegativo() throws Exception {
        JsonNode error = esperar(POST("/api/herramientas", PROVEEDOR, Map.of("categoriaId", 2, "nombre", "Mala " + System.nanoTime(),
                "tarifaDiaria", 1000, "deposito", -5)), 400);
        assertThat(error.get("errores").has("deposito")).isTrue();
    }
}
