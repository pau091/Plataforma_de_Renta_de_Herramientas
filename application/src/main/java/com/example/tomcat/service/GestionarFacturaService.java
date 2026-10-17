package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Factura;
import com.example.tomcat.port.in.GestionarFacturaUseCase;
import com.example.tomcat.repository.FacturaRepositoryPort;

import java.util.List;

public class GestionarFacturaService implements GestionarFacturaUseCase {

    private final FacturaRepositoryPort facturaRepositoryPort;

    public GestionarFacturaService(FacturaRepositoryPort facturaRepositoryPort) {
        this.facturaRepositoryPort = facturaRepositoryPort;
    }

    @Override
    public Factura buscarPorId(Long id) {
        return facturaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("La factura no existe."));
    }

    @Override
    public Factura buscarPorPago(Long pagoId) {
        return facturaRepositoryPort.buscarPorPagoId(pagoId)
                .orElseThrow(() -> new DomainException("No hay factura asociada al pago."));
    }

    @Override
    public Factura buscarPorReserva(Long reservaId) {
        return facturaRepositoryPort.buscarPorReservaId(reservaId)
                .orElseThrow(() -> new DomainException("No hay factura asociada a la reserva."));
    }

    @Override
    public List<Factura> listar() {
        return facturaRepositoryPort.listar();
    }

    @Override
    public List<Factura> listarPorCliente(Long clienteId) {
        return facturaRepositoryPort.listarPorCliente(clienteId);
    }
}
