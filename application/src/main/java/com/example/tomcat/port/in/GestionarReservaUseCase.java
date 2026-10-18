package com.example.tomcat.port.in;

import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.dto.Solicitante;

import java.time.LocalDate;
import java.util.List;

public interface GestionarReservaUseCase {
    /**
     * Lista según el rol: ADMIN ve todas, PROVEEDOR las de sus herramientas y CLIENTE las suyas.
     * Los filtros opcionales (incluido el estado) solo acotan dentro de ese alcance.
     */
    List<Reserva> listar(Solicitante solicitante, Long clienteId, Long proveedorId, Long herramientaId, String estado);
    Reserva buscarPorId(Solicitante solicitante, Long id);
    Reserva aceptar(Solicitante solicitante, Long id);
    Reserva rechazar(Solicitante solicitante, Long id);
    Reserva entregar(Solicitante solicitante, Long id);
    Reserva devolver(Solicitante solicitante, Long id, String estadoEquipo, String observaciones);

    /** Cambia las fechas de una reserva PENDIENTE. Solo el cliente que la hizo o un ADMIN. */
    Reserva modificarFechas(Solicitante solicitante, Long id, LocalDate fechaInicio, LocalDate fechaFin);

    /** Cancela una reserva PENDIENTE o ACEPTADA sin pagar. Solo el cliente que la hizo o un ADMIN. */
    Reserva cancelar(Solicitante solicitante, Long id);
}
