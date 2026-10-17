package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
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
    private final PoliticaVencimiento politica;

    public ProcesarPagoService(
            PagoRepositoryPort pagoRepositoryPort,
            ReservaRepositoryPort reservaRepositoryPort,
            HerramientaRepositoryPort herramientaRepositoryPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            FacturaRepositoryPort facturaRepositoryPort,
            PoliticaVencimiento politica
    ) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.facturaRepositoryPort = facturaRepositoryPort;
        this.politica = politica;
    }

    @Override
    public Pago procesar(Solicitante solicitante, Long reservaId, BigDecimal monto, String metodoPago) {
        Reserva reserva = reservaRepositoryPort.buscarPorId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe."));
        if (!solicitante.esAdmin() && !(solicitante.esCliente() && solicitante.esUsuario(reserva.getClienteId()))) {
            throw new AccesoDenegadoException("Solo el cliente que hizo la reserva puede pagarla.");
        }

        if (pagoRepositoryPort.existePagoCompletado(reservaId)) {
            throw new ConflictoException("La reserva ya tiene un pago completado.");
        }
        if (politica.estaVencida(reserva)) {
            throw new ConflictoException("La reserva venció: el plazo para pagarla era de "
                    + politica.getPlazoMaximo().toMinutes() + " minutos desde que el proveedor la aceptó.");
        }
        // Confirmación automática: al pagar, la reserva ACEPTADA pasa a CONFIRMADA (si no está aceptada, falla aquí).
        reserva.confirmarPago();

        Herramienta herramienta = herramientaRepositoryPort.buscarPorId(reserva.getHerramientaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La herramienta no existe."));
        Usuario cliente = usuarioRepositoryPort.buscarPorId(reserva.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe."));

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
        reservaRepositoryPort.guardar(reserva);
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
