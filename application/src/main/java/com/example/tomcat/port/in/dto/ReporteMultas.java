package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Devoluciones tardías: días de retraso, multa, cuánto cubrió el depósito y lo que queda por cobrar. */
public record ReporteMultas(
        long devolucionesTardias,
        BigDecimal totalMultas,
        BigDecimal saldoPendientePorCobrar,
        List<Multa> items
) {
    public record Multa(
            Long reservaId,
            Long herramientaId,
            String herramientaNombre,
            Long clienteId,
            String clienteNombre,
            LocalDate fechaFin,
            LocalDateTime fechaDevolucion,
            long diasRetraso,
            BigDecimal multa,
            BigDecimal deposito,
            BigDecimal depositoDevuelto,
            BigDecimal saldoPendiente,
            String estadoDeposito
    ) {}
}
