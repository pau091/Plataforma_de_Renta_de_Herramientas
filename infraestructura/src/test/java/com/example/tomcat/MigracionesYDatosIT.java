package com.example.tomcat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integración: migraciones Flyway en MySQL y datos de demostración")
class MigracionesYDatosIT extends IntegracionBase {

    @Autowired
    JdbcTemplate jdbc;

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    @DisplayName("Flyway aplica todas las migraciones (V1–V14) sin errores")
    void migracionesAplicadas() {
        assertThat(jdbc.queryForObject("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1", Integer.class))
                .isEqualTo(14);
        assertThat(contar("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0")).isZero();
    }

    @ParameterizedTest(name = "la tabla {0} tiene al menos 20 registros")
    @ValueSource(strings = {"usuarios", "categorias", "herramientas", "reservas", "pagos", "facturas", "mantenimientos", "usuario_roles"})
    void alMenos20RegistrosPorEntidad(String tabla) {
        assertThat(contar("SELECT COUNT(*) FROM " + tabla)).isGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("Hay al menos 20 proveedores y 20 clientes")
    void proveedoresYClientes() {
        String sql = "SELECT COUNT(*) FROM usuario_roles ur JOIN roles r ON r.id = ur.rol_id WHERE r.nombre = '%s'";
        assertThat(contar(sql.formatted("PROVEEDOR"))).isGreaterThanOrEqualTo(20);
        assertThat(contar(sql.formatted("CLIENTE"))).isGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("Datos coherentes: montos = tarifa × días y pagos completados solo en reservas pagadas")
    void datosCoherentes() {
        assertThat(contar("""
                SELECT COUNT(*) FROM pagos p JOIN reservas r ON r.id = p.reserva_id JOIN herramientas h ON h.id = r.herramienta_id
                WHERE p.monto <> h.tarifa_diaria * (DATEDIFF(r.fecha_fin, r.fecha_inicio) + 1)""")).isZero();
        assertThat(contar("""
                SELECT COUNT(*) FROM pagos p JOIN reservas r ON r.id = p.reserva_id
                WHERE p.estado_pago = 'COMPLETADO' AND r.estado NOT IN ('CONFIRMADA', 'ENTREGADA', 'FINALIZADA')""")).isZero();
        assertThat(contar("""
                SELECT COUNT(*) FROM pagos p LEFT JOIN facturas f ON f.pago_id = p.id
                WHERE f.id IS NULL OR f.total <> p.monto""")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM reservas WHERE estado = 'FINALIZADA' AND fecha_devolucion IS NULL")).isZero();
    }

    @Test
    @DisplayName("Las 30 herramientas de demostración tienen fotografía y el archivo se sirve")
    void herramientasConFotografia() throws Exception {
        var urls = jdbc.queryForList("SELECT imagen_url FROM herramientas WHERE id BETWEEN 1 AND 30", String.class);
        assertThat(urls).hasSize(30).allSatisfy(url -> assertThat(url).startsWith("/images/herramientas/").endsWith(".jpg"));
        for (String url : urls) {
            var respuesta = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url))
                    .andReturn().getResponse();
            assertThat(respuesta.getStatus()).as(url).isEqualTo(200);
            assertThat(respuesta.getContentType()).as(url).isEqualTo("image/jpeg");
        }
    }

    @Test
    @DisplayName("Las restricciones CHECK y las llaves foráneas protegen los datos")
    void restriccionesDeLaBase() {
        assertThatThrownBy(() -> jdbc.update("UPDATE pagos SET monto = -1 WHERE id = 1"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("chk_pagos_monto");
        assertThatThrownBy(() -> jdbc.update("UPDATE reservas SET estado = 'INVENTADO' WHERE id = 1"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("chk_reservas_estado");
        assertThatThrownBy(() -> jdbc.update("UPDATE reservas SET fecha_fin = fecha_inicio - INTERVAL 1 DAY WHERE id = 1"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("chk_reservas_fechas");
        // RESTRICT: no se borra un cliente con reservas (se conserva el historial).
        assertThatThrownBy(() -> jdbc.update("DELETE FROM usuarios WHERE id = 12"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
