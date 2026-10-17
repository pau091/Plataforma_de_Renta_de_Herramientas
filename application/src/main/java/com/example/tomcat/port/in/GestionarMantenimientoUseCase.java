package com.example.tomcat.port.in;

import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.port.in.dto.Solicitante;

import java.math.BigDecimal;
import java.util.List;

public interface GestionarMantenimientoUseCase {
    Mantenimiento registrar(Solicitante solicitante, Long herramientaId, String descripcionProblema, BigDecimal costoReparacion);
    Mantenimiento cerrar(Solicitante solicitante, Long id, BigDecimal costoReparacion);
    /** ADMIN ve todos; PROVEEDOR solo los de sus herramientas. */
    List<Mantenimiento> listar(Solicitante solicitante);
    List<Mantenimiento> listarPorHerramienta(Solicitante solicitante, Long herramientaId);
    List<Mantenimiento> listarPorReserva(Solicitante solicitante, Long reservaId);
}
