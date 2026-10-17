package com.example.tomcat.service;

import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.VencerReservasUseCase;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.util.List;

public class VencerReservasService implements VencerReservasUseCase {

    private static final List<String> ESTADOS_VENCIBLES = List.of("PENDIENTE", "ACEPTADA");

    private final ReservaRepositoryPort reservaRepositoryPort;
    private final PagoRepositoryPort pagoRepositoryPort;
    private final PoliticaVencimiento politica;

    public VencerReservasService(ReservaRepositoryPort reservaRepositoryPort, PagoRepositoryPort pagoRepositoryPort,
                                 PoliticaVencimiento politica) {
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.politica = politica;
    }

    @Override
    public List<Reserva> vencerReservas() {
        return reservaRepositoryPort.listarPorEstados(ESTADOS_VENCIBLES).stream()
                .filter(politica::estaVencida)
                // Protección para datos antiguos: una reserva con pago completado nunca vence.
                .filter(reserva -> !pagoRepositoryPort.existePagoCompletado(reserva.getId()))
                .map(reserva -> {
                    reserva.vencer();
                    return reservaRepositoryPort.guardar(reserva);
                })
                .toList();
    }
}
