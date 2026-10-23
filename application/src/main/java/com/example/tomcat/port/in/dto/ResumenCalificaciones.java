package com.example.tomcat.port.in.dto;

import java.math.BigDecimal;
import java.util.List;

/** Promedio de estrellas (1 decimal) y número de reseñas de cada herramienta y de cada proveedor. */
public record ResumenCalificaciones(List<Promedio> herramientas, List<Promedio> proveedores) {

    /** id = herramientaId o proveedorId, según la lista. */
    public record Promedio(Long id, BigDecimal promedio, long total) {}
}
