package com.example.tomcat.service;

import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;

import java.time.LocalDate;

public class CrearReservaService implements CrearReservaUseCase {

    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;

    public CrearReservaService(
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort
    ) {
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
    }

    @Override
    public Reserva crear(Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario cliente = usuarioRepositoryPort.buscarPorId(clienteId)
                .orElseThrow(() -> new DomainException("El cliente no existe."));
        if (cliente.getRol() != Rol.CLIENTE && cliente.getRol() != Rol.ADMIN) {
            throw new DomainException("Solo un cliente puede crear reservas.");
        }

        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(herramientaId)
                .orElseThrow(() -> new DomainException("La herramienta no existe."));

        if (!herramienta.estaDisponible()) {
            throw new DomainException("La herramienta no se encuentra disponible para renta.");
        }

        if (fechaInicio == null || fechaFin == null) {
            throw new DomainException("Las fechas de la reserva son obligatorias.");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new DomainException("La fecha de inicio no puede ser posterior a la fecha fin.");
        }
        if (reservaRepositoryPort.existeSolape(herramientaId, fechaInicio, fechaFin, null)) {
            throw new DomainException("La herramienta ya tiene una reserva en esas fechas.");
        }

        Reserva reserva = new Reserva(null, clienteId, herramientaId, fechaInicio, fechaFin, "PENDIENTE", null, null, null, null);
        return reservaRepositoryPort.guardar(reserva);
    }
}
