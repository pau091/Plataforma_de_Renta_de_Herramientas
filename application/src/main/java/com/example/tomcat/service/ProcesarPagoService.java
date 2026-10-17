package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Factura;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.ProcesarPagoUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;
import com.example.tomcat.repository.UsuarioRepositoryPort;
import com.example.tomcat.valueobject.Dinero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public class ProcesarPagoService implements ProcesarPagoUseCase {

    private static final Set<String> METODOS = Set.of("TARJETA", "TRANSFERENCIA", "EFECTIVO");

    private final PagoRepositoryPort pagoRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final FacturaRepositoryPort facturaRepositoryPort;

    public ProcesarPagoService(
            PagoRepositoryPort pagoRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            FacturaRepositoryPort facturaRepositoryPort
    ) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.facturaRepositoryPort = facturaRepositoryPort;
    }

    @Override
    public Pago procesar(Solicitante solicitante, Long reservaId, BigDecimal monto, String metodoPago) {
        Reserva reserva = reservaRepositoryPort.buscarPorId(reservaId)
                .orElseThrow(() -> new DomainException("La reserva no existe."));
        if (!solicitante.esAdmin() && !(solicitante.esCliente() && solicitante.esUsuario(reserva.getClienteId()))) {
            throw new AccesoDenegadoException("Solo el cliente que hizo la reserva puede pagarla.");
        }

        if (!reserva.permitePago()) {
            throw new DomainException("La reserva debe estar aceptada para registrar un pago.");
        }
        if (pagoRepositoryPort.existePagoCompletado(reservaId)) {
            throw new DomainException("La reserva ya tiene un pago completado.");
        }

        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(reserva.getHerramientaId())
                .orElseThrow(() -> new DomainException("La herramienta no existe."));
        Usuario cliente = usuarioRepositoryPort.buscarPorId(reserva.getClienteId())
                .orElseThrow(() -> new DomainException("El cliente no existe."));

        BigDecimal esperado = herramienta.getTarifaDiaria().multiply(BigDecimal.valueOf(reserva.diasAlquiler()));
        Dinero dinero = new Dinero(monto);
        if (dinero.getMonto().compareTo(esperado) != 0) {
            throw new DomainException("El monto del pago debe ser " + esperado + " según tarifa y días de alquiler.");
        }

        String metodo = metodoPago == null ? "" : metodoPago.trim().toUpperCase();
        if (!METODOS.contains(metodo)) {
            throw new DomainException("Método de pago no válido. Use TARJETA, TRANSFERENCIA o EFECTIVO.");
        }

        Pago pago = pagoRepositoryPort.guardar(new Pago(null, reservaId, dinero.getMonto(), metodo, "COMPLETADO", null));
        facturaRepositoryPort.guardar(new Factura(
                null,
                "FAC-" + String.format("%06d", pago.getId()),
                pago.getId(),
                reserva.getId(),
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                herramienta.getId(),
                herramienta.getNombre(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                reserva.diasAlquiler(),
                herramienta.getTarifaDiaria(),
                esperado,
                esperado,
                metodo,
                LocalDateTime.now()
        ));
        return pago;
    }
}
