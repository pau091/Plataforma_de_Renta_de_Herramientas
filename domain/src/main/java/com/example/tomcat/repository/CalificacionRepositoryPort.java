package com.example.tomcat.repository;

import com.example.tomcat.model.Calificacion;

import java.util.List;

public interface CalificacionRepositoryPort {
    Calificacion guardar(Calificacion calificacion);
    boolean existePorReserva(Long reservaId);
    List<Calificacion> listar();
    /** Más recientes primero. */
    List<Calificacion> listarPorHerramienta(Long herramientaId);
    List<Calificacion> listarPorCliente(Long clienteId);
}
