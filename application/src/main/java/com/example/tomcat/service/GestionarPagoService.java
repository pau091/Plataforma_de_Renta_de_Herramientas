package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Pago;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.GestionarPagoUseCase;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.FacturaRepositoryPort;
import com.example.tomcat.repository.PagoRepositoryPort;
import com.example.tomcat.repository.ReservaRepositoryPort;

public class GestionarPagoService implements GestionarPagoUseCase {

    private final PagoRepositoryPort pagoRepositoryPort;
    private final ReservaRepositoryPort reservaRepositoryPort;
    private final FacturaRepositoryPort facturaRepositoryPort;
    private final AvisoListaEspera avisoListaEspera;

    public GestionarPagoService(PagoRepositoryPort pagoRepositoryPort, ReservaRepositoryPort reservaRepositoryPort,
                                FacturaRepositoryPort facturaRepositoryPort, AvisoListaEspera avisoListaEspera) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.reservaRepositoryPort = reservaRepositoryPort;
        this.facturaRepositoryPort = facturaRepositoryPort;
        this.avisoListaEspera = avisoListaEspera;
    }

    @Override
    public Pago actualizarMetodo(Solicitante solicitante, Long id, String metodoPago) {
        Pago pago = buscarComoAdmin(solicitante, id);
        pago.cambiarMetodo(metodoPago);
        Pago actualizado = pagoRepositoryPort.guardar(pago);
        facturaRepositoryPort.buscarPorPagoId(id).ifPresent(factura -> {
            factura.cambiarMetodoPago(actualizado.getMetodoPago());
            facturaRepositoryPort.guardar(factura);
        });
        return actualizado;
    }

    @Override
    public Pago reembolsar(Solicitante solicitante, Long id) {
        Pago pago = buscarComoAdmin(solicitante, id);
        Reserva reserva = reservaRepositoryPort.buscarPorId(pago.getReservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva del pago no existe."));
        if ("ENTREGADA".equals(reserva.getEstado()) || "FINALIZADA".equals(reserva.getEstado())) {
            throw new ConflictoException("La herramienta ya fue entregada: el pago no se puede reembolsar.");
        }
        pago.reembolsar();
        reserva.cancelarPorReembolso();

        Pago reembolsado = pagoRepositoryPort.guardar(pago);
        avisoListaEspera.fechasLiberadas(reservaRepositoryPort.guardar(reserva));
        facturaRepositoryPort.buscarPorPagoId(id).ifPresent(factura -> {
            factura.anular();
            factura.registrarLiquidacion(reserva.getGarantia());
            facturaRepositoryPort.guardar(factura);
        });
        return reembolsado;
    }

    private Pago buscarComoAdmin(Solicitante solicitante, Long id) {
        if (!solicitante.esAdmin()) {
            throw new AccesoDenegadoException("Solo el administrador puede modificar o reembolsar pagos.");
        }
        return pagoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pago no existe."));
    }
}
