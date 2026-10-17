package com.example.tomcat.service;

import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.VencerReservasUseCase;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class VencerReservasService implements VencerReservasUseCase {

    private static final List<String> ESTADOS_VENCIBLES = List.of("PENDIENTE", "ACEPTADA");

    private final ReservaRepositoryPort reservaRepositoryPort;
    private final PagoRepositoryPort pagoRepositoryPort;
    private final Clock reloj;

    public VencerReservasService(ReservaRepositoryPort reservaRepositoryPort, PagoRepositoryPort pagoRepositoryPort, Clock reloj) {
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reloj = reloj;
    }

    @Override
    public List<Reserva> vencerReservas() {
        LocalDate hoy = LocalDate.now(reloj);
        return reservaRepositoryPort.listarPorEstadosConInicioAntesDe(ESTADOS_VENCIBLES, hoy).stream()
                // Una reserva ACEPTADA y ya pagada no vence: el proveedor aún debe entregarla.
                .filter(reserva -> !pagoRepositoryPort.existePagoCompletado(reserva.getId()))
                .map(reserva -> {
                    reserva.vencer();
                    return reservaRepositoryPort.guardar(reserva);
                })
                .toList();
    }
}
