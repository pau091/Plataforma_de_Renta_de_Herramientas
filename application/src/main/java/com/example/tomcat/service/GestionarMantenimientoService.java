package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.port.in.GestionarMantenimientoUseCase;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;

import java.math.BigDecimal;
import java.util.List;

public class GestionarMantenimientoService implements GestionarMantenimientoUseCase {

    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;

    public GestionarMantenimientoService(
            MantenimientoRepositoryPort mantenimientoRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort
    ) {
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
    }

    @Override
    public Mantenimiento registrar(Long herramientaId, String descripcionProblema, BigDecimal costoReparacion) {
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        if (descripcionProblema == null || descripcionProblema.isBlank()) {
            throw new DomainException("La descripción del problema es obligatoria.");
        }

        boolean abierto = mantenimientoRepositoryPort.listarPorHerramienta(herramientaId).stream()
                .anyMatch(Mantenimiento::estaAbierto);
        if (abierto) {
            throw new DomainException("La herramienta ya tiene un mantenimiento abierto.");
        }

        herramienta.cambiarEstado("MANTENIMIENTO");
        herramientaRepositoryPort.guardar(herramienta);

        return mantenimientoRepositoryPort.guardar(new Mantenimiento(
                null,
                herramientaId,
                null,
                descripcionProblema.trim(),
                costoReparacion,
                null,
                null
        ));
    }

    @Override
    public Mantenimiento cerrar(Long id, BigDecimal costoReparacion) {
        Mantenimiento mantenimiento = mantenimientoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("El mantenimiento no existe."));
        mantenimiento.cerrar(costoReparacion);
        Mantenimiento cerrado = mantenimientoRepositoryPort.guardar(mantenimiento);

        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(mantenimiento.getHerramientaId())
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        herramienta.cambiarEstado("DISPONIBLE");
        herramientaRepositoryPort.guardar(herramienta);
        return cerrado;
    }

    @Override
    public List<Mantenimiento> listar() {
        return mantenimientoRepositoryPort.listar();
    }

    @Override
    public List<Mantenimiento> listarPorHerramienta(Long herramientaId) {
        herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        return mantenimientoRepositoryPort.listarPorHerramienta(herramientaId);
    }

    @Override
    public List<Mantenimiento> listarPorReserva(Long reservaId) {
        return mantenimientoRepositoryPort.listarPorReserva(reservaId);
    }
}
