package com.example.tomcat.port.in;

import com.example.tomcat.model.Reserva;

import java.util.List;

public interface VencerReservasUseCase {
    /**
     * Marca como VENCIDA cada reserva que no se concretó a tiempo: PENDIENTE sin aceptar o ACEPTADA
     * sin pagar dentro del plazo máximo, o cuya fecha de inicio ya pasó. Devuelve las reservas vencidas.
     */
    List<Reserva> vencerReservas();
}
