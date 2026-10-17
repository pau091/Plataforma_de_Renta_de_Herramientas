package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.util.List;

public class GestionarReservaService implements GestionarReservaUseCase {

    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;

    public GestionarReservaService(
            ReservaRepositoryPort reservaRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            MantenimientoRepositoryPort mantenimientoRepositoryPort
    ) {
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
    }

    @Override
    public List<Reserva> listar() {
        return reservaRepositoryPort.listar();
    }

    @Override
    public List<Reserva> listarPorCliente(Long clienteId) {
        return reservaRepositoryPort.listarPorCliente(clienteId);
    }

    @Override
    public List<Reserva> listarPorProveedor(Long proveedorId) {
        return reservaRepositoryPort.listarPorProveedor(proveedorId);
    }

    @Override
    public List<Reserva> listarPorHerramienta(Long herramientaId) {
        herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        return reservaRepositoryPort.listarPorHerramienta(herramientaId);
    }

    @Override
    public Reserva buscarPorId(Long id) {
        return reservaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("La reserva no existe."));
    }

    @Override
    public Reserva aceptar(Long id) {
        Reserva reserva = buscarPorId(id);
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(reserva.getHerramientaId())
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        if (!herramienta.estaDisponible()) {
            throw new DomainException("La herramienta no está operativa para aceptar la reserva.");
        }
        if (reservaRepositoryPort.existeSolape(reserva.getHerramientaId(), reserva.getFechaInicio(), reserva.getFechaFin(), reserva.getId())) {
            throw new DomainException("La herramienta ya tiene una reserva en esas fechas.");
        }
        reserva.aceptar();
        return reservaRepositoryPort.guardar(reserva);
    }

    @Override
    public Reserva rechazar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.rechazar();
        return reservaRepositoryPort.guardar(reserva);
    }

    @Override
    public Reserva entregar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.entregar();
        return reservaRepositoryPort.guardar(reserva);
    }

    @Override
    public Reserva devolver(Long id, String estadoEquipo, String observaciones) {
        Reserva reserva = buscarPorId(id);
        reserva.devolver(estadoEquipo, observaciones);
        Reserva actualizada = reservaRepositoryPort.guardar(reserva);

        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(reserva.getHerramientaId())
                .orElseThrow(() -> new DomainException("La herramienta no existe."));

        if (actualizada.tieneDano()) {
            herramienta.cambiarEstado("MANTENIMIENTO");
            herramientaRepositoryPort.guardar(herramienta);
            String descripcion = "Daño reportado en devolución de reserva " + reserva.getId();
            if (actualizada.getObservacionesDevolucion() != null) {
                descripcion += ": " + actualizada.getObservacionesDevolucion();
            }
            mantenimientoRepositoryPort.guardar(new Mantenimiento(
                    null,
                    herramienta.getId(),
                    reserva.getId(),
                    descripcion,
                    null,
                    null,
                    null
            ));
        } else if (actualizada.estaPerdida()) {
            herramienta.cambiarEstado("FUERA_DE_SERVICIO");
            herramientaRepositoryPort.guardar(herramienta);
        }

        return actualizada;
    }
}
