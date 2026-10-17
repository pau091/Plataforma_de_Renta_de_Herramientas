package com.example.tomcat.port.in;

import com.example.tomcat.model.Reserva;

import java.util.List;

public interface GestionarReservaUseCase {
    List<Reserva> listar();
    List<Reserva> listarPorCliente(Long clienteId);
    List<Reserva> listarPorProveedor(Long proveedorId);
    List<Reserva> listarPorHerramienta(Long herramientaId);
    Reserva buscarPorId(Long id);
    Reserva aceptar(Long id);
    Reserva rechazar(Long id);
    Reserva entregar(Long id);
    Reserva devolver(Long id, String estadoEquipo, String observaciones);
}
