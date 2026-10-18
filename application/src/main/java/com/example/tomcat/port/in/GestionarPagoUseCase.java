package com.example.tomcat.port.in;

import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.dto.Solicitante;

/**
 * Supervisión de pagos por parte del ADMIN. Los pagos no se borran físicamente: se conservan como historial.
 */
public interface GestionarPagoUseCase {
    /** Corrige el método de pago registrado (y el de su factura). El monto no cambia: lo fija la tarifa. */
    Pago actualizarMetodo(Solicitante solicitante, Long id, String metodoPago);

    /**
     * Reembolsa un pago completado de una reserva que aún no se ha entregado: el pago queda REEMBOLSADO,
     * la reserva CANCELADA (libera las fechas) y la factura ANULADA.
     */
    Pago reembolsar(Solicitante solicitante, Long id);
}
