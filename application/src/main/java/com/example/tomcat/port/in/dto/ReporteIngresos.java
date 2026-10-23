package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ingresos de la plataforma. El depósito de garantía NO es ingreso mientras está en custodia: solo cuenta la parte
 * que se aplicó a multas o daños.
 * totalIngresos = ingresosAlquiler + ingresosMultas + ingresosDanos.
 */
public record ReporteIngresos(
        BigDecimal totalIngresos,
        long pagosCompletados,
        List<IngresoPorMetodo> porMetodo,
        BigDecimal ingresosAlquiler,
        BigDecimal ingresosMultas,
        BigDecimal ingresosDanos,
        BigDecimal depositosEnCustodia,
        BigDecimal depositosDevueltos,
        BigDecimal saldoPendientePorCobrar
) {
    /** Alquileres cobrados por método de pago (sin el depósito). */
    public record IngresoPorMetodo(String metodoPago, long cantidad, BigDecimal total) {}
}
