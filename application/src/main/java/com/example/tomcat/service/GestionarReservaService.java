package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.util.List;

public class GestionarReservaService implements GestionarReservaUseCase {

    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;
    private final ControlAcceso controlAcceso;

    public GestionarReservaService(
            ReservaRepositoryPort reservaRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            MantenimientoRepositoryPort mantenimientoRepositoryPort,
            ControlAcceso controlAcceso
    ) {
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
        this.controlAcceso = controlAcceso;
    }

    @Override
    public List<Reserva> listar(Solicitante solicitante, Long clienteId, Long proveedorId, Long herramientaId) {
        List<Reserva> reservas;
        if (solicitante.esCliente()) {
            if (clienteId != null && !solicitante.esUsuario(clienteId)) {
                throw new AccesoDenegadoException("Solo puede consultar sus propias reservas.");
            }
            reservas = reservaRepositoryPort.listarPorCliente(solicitante.id());
        } else if (solicitante.esProveedor()) {
            if (proveedorId != null && !solicitante.esUsuario(proveedorId)) {
                throw new AccesoDenegadoException("Solo puede consultar las reservas de sus herramientas.");
            }
            reservas = reservaRepositoryPort.listarPorProveedor(solicitante.id());
        } else if (proveedorId != null) {
            reservas = reservaRepositoryPort.listarPorProveedor(proveedorId);
        } else if (clienteId != null) {
            reservas = reservaRepositoryPort.listarPorCliente(clienteId);
        } else {
            reservas = reservaRepositoryPort.listar();
        }

        if (herramientaId != null) {
            herramientaRepositoryPort.buscarPorId(herramientaId)
                    .orElseThrow(() -> new DomainException("La herramienta no existe."));
            reservas = reservas.stream().filter(r -> herramientaId.equals(r.getHerramientaId())).toList();
        }
        if (clienteId != null) {
            reservas = reservas.stream().filter(r -> clienteId.equals(r.getClienteId())).toList();
        }
        return reservas;
    }

    @Override
    public Reserva buscarPorId(Solicitante solicitante, Long id) {
        Reserva reserva = buscar(id);
        controlAcceso.verificarAccesoReserva(solicitante, reserva);
        return reserva;
    }

    @Override
    public Reserva aceptar(Solicitante solicitante, Long id) {
        Reserva reserva = buscarParaGestionar(solicitante, id);
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
    public Reserva rechazar(Solicitante solicitante, Long id) {
        Reserva reserva = buscarParaGestionar(solicitante, id);
        reserva.rechazar();
        return reservaRepositoryPort.guardar(reserva);
    }

    @Override
    public Reserva entregar(Solicitante solicitante, Long id) {
        Reserva reserva = buscarParaGestionar(solicitante, id);
        reserva.entregar();
        return reservaRepositoryPort.guardar(reserva);
    }

    @Override
    public Reserva devolver(Solicitante solicitante, Long id, String estadoEquipo, String observaciones) {
        Reserva reserva = buscarParaGestionar(solicitante, id);
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

    private Reserva buscar(Long id) {
        return reservaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new DomainException("La reserva no existe."));
    }

    private Reserva buscarParaGestionar(Solicitante solicitante, Long id) {
        Reserva reserva = buscar(id);
        controlAcceso.verificarGestionReserva(solicitante, reserva);
        return reserva;
    }
}
