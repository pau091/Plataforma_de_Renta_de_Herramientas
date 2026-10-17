package com.example.tomcat.port.in;

import com.example.tomcat.model.Reserva;
import java.time.LocalDate;

public interface CrearReservaUseCase {
    Reserva crear(Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin);
}