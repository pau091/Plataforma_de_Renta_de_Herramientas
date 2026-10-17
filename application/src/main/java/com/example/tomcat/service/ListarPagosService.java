package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.util.List;

public class ListarPagosService implements ListarPagosUseCase {

    private final PagoRepositoryPort pagoRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;

    public ListarPagosService(PagoRepositoryPort pagoRepositoryPort, ReservaRepositoryPort reservaRepositoryPort) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
    }

    @Override
    public List<Pago> listar() {
        return pagoRepositoryPort.listar();
    }

    @Override
    public List<Pago> historialPorReserva(Long reservaId) {
        reservaRepositoryPort.buscarPorId(reservaId)
                .orElseThrow(() -> new DomainException("La reserva no existe."));
        return pagoRepositoryPort.listarPorReserva(reservaId);
    }

    @Override
    public Pago buscarPorId(Long id) {
        return pagoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("El pago no existe."));
    }
}
