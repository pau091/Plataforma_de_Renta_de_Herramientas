package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rentabilidad = ingresos por alquileres pagados - costo de los mantenimientos (reparaciones).
 * El margen es la utilidad sobre los ingresos en porcentaje; es null si no hubo ingresos.
 */
public record ReporteRentabilidad(
        BigDecimal ingresosTotales,
        BigDecimal costoMantenimientoTotal,
        BigDecimal utilidadTotal,
        BigDecimal margenPorcentaje,
        List<Item> items
) {
    public record Item(
            Long herramientaId,
            String nombre,
            Long proveedorId,
            String proveedorNombre,
            long alquileresPagados,
            BigDecimal ingresos,
            BigDecimal costoMantenimiento,
            BigDecimal utilidad,
            BigDecimal margenPorcentaje
    ) {}
}
