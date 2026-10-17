package com.example.tomcat.service;

import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.util.List;

public class ListarPagosService implements ListarPagosUseCase {

    private final PagoRepositoryPort pagoRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final ControlAcceso controlAcceso;

    public ListarPagosService(PagoRepositoryPort pagoRepositoryPort, ReservaRepositoryPort reservaRepositoryPort,
                              ControlAcceso controlAcceso) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.controlAcceso = controlAcceso;
    }

    @Override
    public List<Pago> listar(Solicitante solicitante) {
        if (solicitante.esAdmin()) {
            return pagoRepositoryPort.listar();
        }
        List<Reserva> reservas = solicitante.esProveedor()
                ? reservaRepositoryPort.listarPorProveedor(solicitante.id())
                : reservaRepositoryPort.listarPorCliente(solicitante.id());
        return pagoRepositoryPort.listarPorReservas(reservas.stream().map(Reserva::getId).toList());
    }

    @Override
    public List<Pago> historialPorReserva(Solicitante solicitante, Long reservaId) {
        verificarReserva(solicitante, reservaId);
        return pagoRepositoryPort.listarPorReserva(reservaId);
    }

    @Override
    public Pago buscarPorId(Solicitante solicitante, Long id) {
        Pago pago = pagoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pago no existe."));
        verificarReserva(solicitante, pago.getReservaId());
        return pago;
    }

    private void verificarReserva(Solicitante solicitante, Long reservaId) {
        Reserva reserva = reservaRepositoryPort.buscarPorId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe."));
        controlAcceso.verificarAccesoReserva(solicitante, reserva);
    }
}
