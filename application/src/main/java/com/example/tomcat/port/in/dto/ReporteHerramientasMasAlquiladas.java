package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteHerramientasMasAlquiladas(List<Item> items) {
    public record Item(Long herramientaId, String nombre, String estado, long alquileres, BigDecimal ingresos) {}
}
