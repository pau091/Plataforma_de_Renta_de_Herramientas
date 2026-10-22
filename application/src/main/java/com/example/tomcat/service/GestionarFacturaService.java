package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Factura;
import com.example.tomcat.port.in.GestionarFacturaUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.FacturaRepositoryPort;

import java.util.List;

public class GestionarFacturaService implements GestionarFacturaUseCase {

    private final FacturaRepositoryPort facturaRepositoryPort;
    private final ControlAcceso controlAcceso;

    public GestionarFacturaService(FacturaRepositoryPort facturaRepositoryPort, ControlAcceso controlAcceso) {
        this.facturaRepositoryPort = facturaRepositoryPort;
        this.controlAcceso = controlAcceso;
    }

    @Override
    public Factura buscarPorId(Solicitante solicitante, Long id) {
        return verificar(solicitante, facturaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La factura no existe.")));
    }

    @Override
    public Factura buscarPorPago(Solicitante solicitante, Long pagoId) {
        return verificar(solicitante, facturaRepositoryPort.buscarPorPagoId(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay factura asociada al pago.")));
    }

    @Override
    public Factura buscarPorReserva(Solicitante solicitante, Long reservaId) {
        return verificar(solicitante, facturaRepositoryPort.buscarPorReservaId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay factura asociada a la reserva.")));
    }

    @Override
    public List<Factura> listar(Solicitante solicitante, Long clienteId) {
        if (solicitante.esCliente()) {
            if (clienteId != null && !solicitante.esUsuario(clienteId)) {
                throw new AccesoDenegadoException("Solo puede consultar sus propias facturas.");
            }
            return facturaRepositoryPort.listarPorCliente(solicitante.id());
        }
        if (solicitante.esProveedor() && clienteId != null && solicitante.esUsuario(clienteId)) {
            // Facturas de lo que el proveedor alquiló como cliente.
            return facturaRepositoryPort.listarPorCliente(solicitante.id());
        }
        if (solicitante.esProveedor()) {
            List<Factura> facturas = facturaRepositoryPort.listarPorHerramientas(
                    controlAcceso.idsHerramientasDelProveedor(solicitante.id()));
            return clienteId == null ? facturas
                    : facturas.stream().filter(f -> clienteId.equals(f.getClienteId())).toList();
        }
        return clienteId != null ? facturaRepositoryPort.listarPorCliente(clienteId) : facturaRepositoryPort.listar();
    }

    private Factura verificar(Solicitante solicitante, Factura factura) {
        controlAcceso.verificarAccesoFactura(solicitante, factura);
        return factura;
    }
}
