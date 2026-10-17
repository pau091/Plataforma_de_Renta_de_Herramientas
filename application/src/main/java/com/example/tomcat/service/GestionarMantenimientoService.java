package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.GestionarMantenimientoUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.math.BigDecimal;
import java.util.List;

public class GestionarMantenimientoService implements GestionarMantenimientoUseCase {

    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final ControlAcceso controlAcceso;

    public GestionarMantenimientoService(
            MantenimientoRepositoryPort mantenimientoRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            ControlAcceso controlAcceso
    ) {
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.controlAcceso = controlAcceso;
    }

    @Override
    public Mantenimiento registrar(Solicitante solicitante, Long herramientaId, String descripcionProblema, BigDecimal costoReparacion) {
        Herramienta herramienta = buscarHerramientaPropia(solicitante, herramientaId);
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
    public Mantenimiento cerrar(Solicitante solicitante, Long id, BigDecimal costoReparacion) {
        Mantenimiento mantenimiento = mantenimientoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("El mantenimiento no existe."));
        Herramienta herramienta = buscarHerramientaPropia(solicitante, mantenimiento.getHerramientaId());

        mantenimiento.cerrar(costoReparacion);
        Mantenimiento cerrado = mantenimientoRepositoryPort.guardar(mantenimiento);

        herramienta.cambiarEstado("DISPONIBLE");
        herramientaRepositoryPort.guardar(herramienta);
        return cerrado;
    }

    @Override
    public List<Mantenimiento> listar(Solicitante solicitante) {
        if (solicitante.esAdmin()) {
            return mantenimientoRepositoryPort.listar();
        }
        return mantenimientoRepositoryPort.listarPorHerramientas(controlAcceso.idsHerramientasDelProveedor(solicitante.id()));
    }

    @Override
    public List<Mantenimiento> listarPorHerramienta(Solicitante solicitante, Long herramientaId) {
        buscarHerramientaPropia(solicitante, herramientaId);
        return mantenimientoRepositoryPort.listarPorHerramienta(herramientaId);
    }

    @Override
    public List<Mantenimiento> listarPorReserva(Solicitante solicitante, Long reservaId) {
        Reserva reserva = reservaRepositoryPort.buscarPorId(reservaId)
                .orElseThrow(() -> new DomainException("La reserva no existe."));
        controlAcceso.verificarGestionReserva(solicitante, reserva);
        return mantenimientoRepositoryPort.listarPorReserva(reservaId);
    }

    private Herramienta buscarHerramientaPropia(Solicitante solicitante, Long herramientaId) {
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        controlAcceso.verificarPropietarioHerramienta(solicitante, herramienta);
        return herramienta;
    }
}
