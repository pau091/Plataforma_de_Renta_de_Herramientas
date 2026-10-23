package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.MantenimientoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class GestionarReservaService implements GestionarReservaUseCase {

    private static final Set<String> ESTADOS = Set.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "ENTREGADA",
            "FINALIZADA", "RECHAZADA", "VENCIDA", "CANCELADA");

    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final MantenimientoRepositoryPort mantenimientoRepositoryPort;
    private final ControlAcceso controlAcceso;
    private final PoliticaVencimiento politica;
    private final FacturaRepositoryPort facturaRepositoryPort;
    /** Recargo sobre la tarifa por cada día de retraso al devolver (0,5 = 50%); app.reservas.recargo-mora. */
    private final BigDecimal recargoMora;
    private final AvisoListaEspera avisoListaEspera;

    public GestionarReservaService(
            ReservaRepositoryPort reservaRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            MantenimientoRepositoryPort mantenimientoRepositoryPort,
            ControlAcceso controlAcceso,
            PoliticaVencimiento politica,
            FacturaRepositoryPort facturaRepositoryPort,
            BigDecimal recargoMora,
            AvisoListaEspera avisoListaEspera
    ) {
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.mantenimientoRepositoryPort = mantenimientoRepositoryPort;
        this.controlAcceso = controlAcceso;
        this.politica = politica;
        this.facturaRepositoryPort = facturaRepositoryPort;
        this.recargoMora = recargoMora;
        this.avisoListaEspera = avisoListaEspera;
    }

    @Override
    public List<Reserva> listar(Solicitante solicitante, Long clienteId, Long proveedorId, Long herramientaId, String estado) {
        List<Reserva> reservas;
        if (solicitante.esCliente()) {
            if (clienteId != null && !solicitante.esUsuario(clienteId)) {
                throw new AccesoDenegadoException("Solo puede consultar sus propias reservas.");
            }
            reservas = reservaRepositoryPort.listarPorCliente(solicitante.id());
        } else if (solicitante.esProveedor() && clienteId != null && solicitante.esUsuario(clienteId)) {
            // El proveedor consulta las reservas que hizo como cliente (herramientas de otros proveedores).
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
                    .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));
            reservas = reservas.stream().filter(r -> herramientaId.equals(r.getHerramientaId())).toList();
        }
        if (clienteId != null) {
            reservas = reservas.stream().filter(r -> clienteId.equals(r.getClienteId())).toList();
        }
        if (estado != null && !estado.isBlank()) {
            String estadoBuscado = estado.trim().toUpperCase();
            if (!ESTADOS.contains(estadoBuscado)) {
                throw new DomainException("Estado de reserva no válido. Use uno de: " + String.join(", ", ESTADOS.stream().sorted().toList()) + ".");
            }
            reservas = reservas.stream().filter(r -> estadoBuscado.equals(r.getEstado())).toList();
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
        Herramienta herramienta = herramientaRepositoryPort.bloquearPorId(reserva.getHerramientaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));
        if (!herramienta.estaDisponible()) {
            throw new DomainException("La herramienta no está operativa para aceptar la reserva.");
        }
        if (reservaRepositoryPort.existeSolape(reserva.getHerramientaId(), reserva.getFechaInicio(), reserva.getFechaFin(), reserva.getId())) {
            throw new ConflictoException("La herramienta ya tiene una reserva en esas fechas.");
        }
        if (politica.estaVencida(reserva)) {
            throw new ConflictoException("La reserva venció: el plazo para aceptarla era de "
                    + politica.getPlazoMaximo().toMinutes() + " minutos desde que se creó.");
        }
        reserva.aceptar(politica.ahoraUtc());
        return reservaRepositoryPort.guardar(reserva);
    }

    @Override
    public Reserva rechazar(Solicitante solicitante, Long id) {
        Reserva reserva = buscarParaGestionar(solicitante, id);
        reserva.rechazar();
        Reserva rechazada = reservaRepositoryPort.guardar(reserva);
        avisoListaEspera.fechasLiberadas(rechazada);
        return rechazada;
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
        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(reserva.getHerramientaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));

        // Si se devuelve después de la fecha fin, cada día de retraso genera una multa (tarifa + recargo) que se
        // descuenta del depósito, igual que el costo de los daños.
        reserva.devolver(estadoEquipo, observaciones, politica.hoy(), herramienta.getTarifaDiaria(), recargoMora);
        Reserva actualizada = reservaRepositoryPort.guardar(reserva);
        reflejarEnFactura(actualizada);

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

    @Override
    public Reserva modificarFechas(Solicitante solicitante, Long id, LocalDate fechaInicio, LocalDate fechaFin) {
        Reserva reserva = buscarComoTitular(solicitante, id);
        if (politica.estaVencida(reserva)) {
            throw new ConflictoException("La reserva venció: el proveedor no la aceptó dentro del plazo. Cree una nueva.");
        }
        reserva.modificarFechas(fechaInicio, fechaFin, politica.hoy());
        herramientaRepositoryPort.bloquearPorId(reserva.getHerramientaId());
        if (reservaRepositoryPort.existeSolape(reserva.getHerramientaId(), fechaInicio, fechaFin, reserva.getId())) {
            throw new ConflictoException("La herramienta ya tiene una reserva en esas fechas.");
        }
        return reservaRepositoryPort.guardar(reserva);
    }

    @Override
    public Reserva cancelar(Solicitante solicitante, Long id) {
        Reserva reserva = buscarComoTitular(solicitante, id);
        reserva.cancelar();
        Reserva cancelada = reservaRepositoryPort.guardar(reserva);
        // Las fechas quedaron libres: se avisa al primero de la lista de espera.
        avisoListaEspera.fechasLiberadas(cancelada);
        return cancelada;
    }

    private Reserva buscar(Long id) {
        return reservaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe."));
    }

    /** La factura de la reserva muestra la liquidación del depósito (multa, daños, lo devuelto y el saldo). */
    private void reflejarEnFactura(Reserva reserva) {
        facturaRepositoryPort.buscarPorReservaId(reserva.getId()).ifPresent(factura -> {
            factura.registrarLiquidacion(reserva.getGarantia());
            facturaRepositoryPort.guardar(factura);
        });
    }

    private Reserva buscarComoTitular(Solicitante solicitante, Long id) {
        Reserva reserva = buscar(id);
        controlAcceso.verificarTitularReserva(solicitante, reserva);
        return reserva;
    }

    private Reserva buscarParaGestionar(Solicitante solicitante, Long id) {
        Reserva reserva = buscar(id);
        controlAcceso.verificarGestionReserva(solicitante, reserva);
        return reserva;
    }
}
