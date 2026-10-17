package com.example.tomcat.port.in;

import com.example.tomcat.model.Factura;
import com.example.tomcat.port.in.dto.Solicitante;

import java.util.List;

public interface GestionarFacturaUseCase {
    Factura buscarPorId(Solicitante solicitante, Long id);
    Factura buscarPorPago(Solicitante solicitante, Long pagoId);
    Factura buscarPorReserva(Solicitante solicitante, Long reservaId);
    /**
     * ADMIN ve todas (o las de un cliente si indica clienteId); PROVEEDOR las de sus herramientas;
     * CLIENTE solo las suyas.
     */
    List<Factura> listar(Solicitante solicitante, Long clienteId);
}
