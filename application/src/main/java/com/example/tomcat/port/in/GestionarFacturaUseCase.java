package com.example.tomcat.port.in;

import com.example.tomcat.model.Factura;

import java.util.List;

public interface GestionarFacturaUseCase {
    Factura buscarPorId(Long id);
    Factura buscarPorPago(Long pagoId);
    Factura buscarPorReserva(Long reservaId);
    List<Factura> listar();
    List<Factura> listarPorCliente(Long clienteId);
}
