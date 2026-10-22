package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;

import java.time.LocalDate;

public class CrearReservaService implements CrearReservaUseCase {

    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final PoliticaVencimiento politica;

    public CrearReservaService(
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            PoliticaVencimiento politica
    ) {
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.politica = politica;
    }

    @Override
    public Reserva crear(Solicitante solicitante, Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {
        Long clienteReserva = resolverCliente(solicitante, clienteId);

        // Bloquea la herramienta: si otro cliente la está reservando en este instante, se espera a que termine
        // y la validación de cruce de fechas ya ve su reserva.
        Herramienta herramienta = herramientaRepositoryPort.bloquearPorId(herramientaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));

        if (!herramienta.estaDisponible()) {
            throw new DomainException("La herramienta no se encuentra disponible para renta.");
        }

        Reserva.validarFechas(fechaInicio, fechaFin, politica.hoy());
        if (reservaRepositoryPort.existeSolape(herramientaId, fechaInicio, fechaFin, null)) {
            throw new ConflictoException("La herramienta ya tiene una reserva en esas fechas.");
        }

        // Queda PENDIENTE: el proveedor tiene el plazo máximo (1 hora por defecto) para aceptarla o rechazarla.
        Reserva reserva = new Reserva(null, clienteReserva, herramientaId, fechaInicio, fechaFin, "PENDIENTE",
                null, null, null, null, politica.ahoraUtc(), null);
        return reservaRepositoryPort.guardar(reserva);
    }

    private Long resolverCliente(Solicitante solicitante, Long clienteId) {
        if (solicitante.esCliente()) {
            // El cliente siempre reserva a su nombre, sin importar el clienteId enviado.
            return solicitante.id();
        }
        if (!solicitante.esAdmin()) {
            throw new AccesoDenegadoException("Solo clientes o administradores pueden crear reservas.");
        }
        if (clienteId == null) {
            throw new DomainException("Debe indicar el clienteId para quien se crea la reserva.");
        }
        Usuario cliente = usuarioRepositoryPort.buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe."));
        if (cliente.getRol() != Rol.CLIENTE) {
            throw new DomainException("El usuario indicado no es un cliente.");
        }
        return clienteId;
    }
}
