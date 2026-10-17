package com.example.tomcat.port.in.dto;

import com.example.tomcat.model.Reserva;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Resumen del panel de control; su contenido depende del rol del usuario autenticado. */
public sealed interface Panel permits Panel.Cliente, Panel.Proveedor, Panel.Admin {

    String rol();

    /** Historial y próximas reservas del cliente. */
    record Cliente(
            String rol,
            Map<String, Long> reservasPorEstado,
            List<Reserva> proximasReservas,
            BigDecimal totalPagado,
            long facturas
    ) implements Panel {}

    /** Inventario y solicitudes pendientes de respuesta del proveedor. */
    record Proveedor(
            String rol,
            long herramientas,
            Map<String, Long> herramientasPorEstado,
            Map<String, Long> reservasPorEstado,
            List<Reserva> solicitudesPendientes,
            BigDecimal ingresos,
            long mantenimientosAbiertos
    ) implements Panel {}

    /** Visión global de la plataforma. */
    record Admin(
            String rol,
            long clientes,
            long proveedores,
            Map<String, Long> herramientasPorEstado,
            Map<String, Long> reservasPorEstado,
            BigDecimal ingresos,
            long pagosCompletados,
            long mantenimientosAbiertos
    ) implements Panel {}
}
