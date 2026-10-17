package com.example.tomcat.repository;

import com.example.tomcat.model.Factura;

import java.util.List;
import java.util.Optional;

public interface FacturaRepositoryPort {
    Factura guardar(Factura factura);
    Optional<Factura> buscarPorId(Long id);
    Optional<Factura> buscarPorPagoId(Long pagoId);
    Optional<Factura> buscarPorReservaId(Long reservaId);
    List<Factura> listar();
    List<Factura> listarPorCliente(Long clienteId);
}
