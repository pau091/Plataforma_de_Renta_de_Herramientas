package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteIngresos(
        BigDecimal totalIngresos,
        long pagosCompletados,
        List<IngresoPorMetodo> porMetodo
) {
    public record IngresoPorMetodo(String metodoPago, long cantidad, BigDecimal total) {}
}
