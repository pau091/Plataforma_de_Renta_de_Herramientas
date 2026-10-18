package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteClientesFrecuentes(List<ClienteFrecuente> items) {
    public record ClienteFrecuente(Long clienteId, String nombre, String email, long reservas, BigDecimal totalPagado) {}
}
