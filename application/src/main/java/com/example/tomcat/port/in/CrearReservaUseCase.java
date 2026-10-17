package com.example.tomcat.port.in;

import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.dto.Solicitante;

import java.time.LocalDate;

public interface CrearReservaUseCase {
    /**
     * Si el solicitante es CLIENTE la reserva queda a su nombre; si es ADMIN debe indicar el clienteId.
     * La reserva nace PENDIENTE hasta que el proveedor la acepte o la rechace.
     */
    Reserva crear(Solicitante solicitante, Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin);
}
