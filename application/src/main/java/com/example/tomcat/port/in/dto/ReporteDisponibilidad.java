package com.example.tomcat.port.in.dto;

import java.util.List;

public record ReporteDisponibilidad(
        long total,
        long disponibles,
        long enMantenimiento,
        long fueraDeServicio,
        List<Item> items
) {
    public record Item(Long id, String nombre, String estado, Long categoriaId, Long proveedorId, boolean operativa) {}
}
