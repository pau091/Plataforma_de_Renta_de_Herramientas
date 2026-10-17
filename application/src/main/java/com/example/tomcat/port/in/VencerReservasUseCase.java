package com.example.tomcat.port.in;

import com.example.tomcat.model.Reserva;

import java.util.List;

public interface VencerReservasUseCase {
    /**
     * Marca como VENCIDA cada reserva cuya fecha de inicio ya pasó y que sigue PENDIENTE
     * o ACEPTADA sin pago completado. Devuelve las reservas vencidas en esta ejecución.
     */
    List<Reserva> vencerReservas();
}
