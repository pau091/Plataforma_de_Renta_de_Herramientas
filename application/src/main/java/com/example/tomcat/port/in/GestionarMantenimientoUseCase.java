package com.example.tomcat.port.in;

import com.example.tomcat.model.Mantenimiento;

import java.math.BigDecimal;
import java.util.List;

public interface GestionarMantenimientoUseCase {
    Mantenimiento registrar(Long herramientaId, String descripcionProblema, BigDecimal costoReparacion);
    Mantenimiento cerrar(Long id, BigDecimal costoReparacion);
    List<Mantenimiento> listar();
    List<Mantenimiento> listarPorHerramienta(Long herramientaId);
    List<Mantenimiento> listarPorReserva(Long reservaId);
}
