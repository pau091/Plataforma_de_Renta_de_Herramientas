package com.example.tomcat;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integración: cambio de rol por el admin principal y proveedor que alquila como cliente")
class RolesIT extends IntegracionBase {

    private JsonNode cambiarRol(String quien, long usuarioId, String rol, int status) throws Exception {
        return esperar(PATCH("/api/usuarios/" + usuarioId + "/rol", quien, Map.of("rol", rol)), status);
    }

    private static boolean contieneId(JsonNode lista, long id) {
        return StreamSupport.stream(lista.spliterator(), false).anyMatch(n -> n.get("id").asLong() == id);
    }

    @Test
    @DisplayName("El admin principal asciende CLIENTE → PROVEEDOR → ADMIN y lo baja; el rol aplica sin volver a iniciar sesión")
    void ascensoYDescenso() throws Exception {
        String correo = "ascenso." + System.nanoTime() + "@prueba.test";
        long id = esperar(POST("/api/usuarios", ADMIN, Map.of("nombre", "Conde", "email", correo,
                "password", CLAVE, "rol", "CLIENTE")), 201).get("id").asLong();
        esperar(GET("/api/mantenimientos", correo), 403);

        assertThat(cambiarRol(ADMIN, id, "PROVEEDOR", 200).get("rol").asText()).isEqualTo("PROVEEDOR");
        esperar(GET("/api/mantenimientos", correo), 200);   // mismo token: el filtro JWT lee el rol de la base

        assertThat(cambiarRol(ADMIN, id, "ADMIN", 200).get("rol").asText()).isEqualTo("ADMIN");
        esperar(GET("/api/reportes/ingresos", correo), 200);
        assertThat(contieneId(esperar(GET("/api/usuarios?rol=ADMIN", ADMIN), 200), id)).isTrue();

        // Un admin ascendido gestiona la plataforma, pero no reparte roles.
        JsonNode r = cambiarRol(correo, 12, "PROVEEDOR", 403);
        assertThat(r.get("message").asText()).contains("administrador principal");

        cambiarRol(ADMIN, id, "CLIENTE", 200);
        esperar(GET("/api/reportes/ingresos", correo), 403);
    }

    @Test
    @DisplayName("Protecciones: admin principal intocable, proveedor con herramientas no pasa a CLIENTE, solo ADMIN entra")
    void protecciones() throws Exception {
        assertThat(cambiarRol(ADMIN, 1, "CLIENTE", 409).get("message").asText()).contains("administrador principal");
        assertThat(cambiarRol(ADMIN, 2, "CLIENTE", 409).get("message").asText()).contains("herramientas registradas");
        cambiarRol(ADMIN, 12, "JEFE", 400);
        cambiarRol(ADMIN, 12, "CLIENTE", 400);   // ya es cliente
        cambiarRol(ADMIN, 999999, "ADMIN", 404);
        cambiarRol(CLIENTE, 13, "ADMIN", 403);
        cambiarRol(PROVEEDOR, 13, "ADMIN", 403);
    }

    @Test
    @DisplayName("Un proveedor alquila la herramienta de otro: reserva, paga y descarga su factura; no reserva las suyas")
    void proveedorComoCliente() throws Exception {
        long deMaria = esperar(POST("/api/herramientas", OTRO_PROVEEDOR, Map.of("categoriaId", 2,
                "nombre", "Pulidora de Maria " + System.nanoTime(), "descripcion", "Para alquilar a otro proveedor",
                "tarifaDiaria", 20000)), 201).get("id").asLong();
        long propia = nuevaHerramienta("Herramienta propia", 10000);

        JsonNode error = esperar(POST("/api/reservas", PROVEEDOR, Map.of("herramientaId", propia,
                "fechaInicio", dia(60), "fechaFin", dia(61))), 400);
        assertThat(error.get("message").asText()).contains("propias herramientas");

        JsonNode reserva = esperar(POST("/api/reservas", PROVEEDOR, Map.of("herramientaId", deMaria,
                "fechaInicio", dia(60), "fechaFin", dia(61))), 201);
        long id = reserva.get("id").asLong();
        assertThat(reserva.get("clienteId").asLong()).isEqualTo(2L);

        esperar(POST("/api/reservas/" + id + "/aceptar", OTRO_PROVEEDOR, null), 200);
        esperar(POST("/api/pagos", PROVEEDOR, Map.of("reservaId", id, "monto", 40000, "metodoPago", "EFECTIVO")), 201);

        // Como cliente ve su reserva con ?clienteId=<su id>; sin el filtro sigue viendo las reservas de sus herramientas.
        assertThat(contieneId(esperar(GET("/api/reservas?clienteId=2", PROVEEDOR), 200), id)).isTrue();
        assertThat(contieneId(esperar(GET("/api/reservas", PROVEEDOR), 200), id)).isFalse();
        assertThat(contieneId(esperar(GET("/api/reservas", OTRO_PROVEEDOR), 200), id)).isTrue();

        JsonNode facturas = esperar(GET("/api/facturas?clienteId=2", PROVEEDOR), 200);
        JsonNode factura = StreamSupport.stream(facturas.spliterator(), false)
                .filter(f -> f.get("reservaId").asLong() == id).findFirst().orElseThrow();
        byte[] pdf = mvc.perform(GET("/api/facturas/" + factura.get("id").asLong() + "/descargar", PROVEEDOR))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(textoDelPdf(pdf)).contains("Carlos", "$ 40.000 COP");
    }
}
