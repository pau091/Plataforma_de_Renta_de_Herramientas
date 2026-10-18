package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Devoluciones con incidencia (equipo DANADO o PERDIDO) y el mantenimiento que generaron, si lo hay.
 */
public record ReporteIncidencias(long total, long danadas, long perdidas, List<Item> items) {
    public record Item(
            Long reservaId,
            Long herramientaId,
            String herramientaNombre,
            String estadoHerramienta,
            Long proveedorId,
            Long clienteId,
            String clienteNombre,
            String estadoDevolucion,
            String observaciones,
            LocalDateTime fechaDevolucion,
            Long mantenimientoId,
            Boolean mantenimientoAbierto,
            BigDecimal costoReparacion
    ) {}
}
