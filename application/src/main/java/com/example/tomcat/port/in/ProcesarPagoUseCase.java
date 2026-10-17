package com.example.tomcat.port.in;

import com.example.tomcat.model.Pago;
import java.math.BigDecimal;

public interface ProcesarPagoUseCase {
    Pago procesar(Long reservaId, BigDecimal monto, String metodoPago);
}