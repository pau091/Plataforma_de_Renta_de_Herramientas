package com.example.tomcat.repository;

import com.example.tomcat.model.Mantenimiento;

import java.util.List;
import java.util.Optional;

public interface MantenimientoRepositoryPort {
    Mantenimiento guardar(Mantenimiento mantenimiento);
    Optional<Mantenimiento> buscarPorId(Long id);
    List<Mantenimiento> listar();
    List<Mantenimiento> listarPorHerramienta(Long herramientaId);
    List<Mantenimiento> listarPorReserva(Long reservaId);
}
