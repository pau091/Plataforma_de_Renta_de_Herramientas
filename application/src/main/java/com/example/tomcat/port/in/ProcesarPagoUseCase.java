package com.example.tomcat.port.in;

import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.dto.Solicitante;

import java.math.BigDecimal;

public interface ProcesarPagoUseCase {
    /** Solo el cliente dueño de la reserva (o un ADMIN) puede pagarla. */
    Pago procesar(Solicitante solicitante, Long reservaId, BigDecimal monto, String metodoPago);
}
