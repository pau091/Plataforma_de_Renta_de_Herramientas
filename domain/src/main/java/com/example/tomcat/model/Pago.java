package com.example.tomcat.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago {
    private Long id;
    private Long reservaId;
    private BigDecimal monto;
    private String metodoPago; // TARJETA, TRANSFERENCIA, EFECTIVO
    private String estadoPago; // PENDIENTE, COMPLETADO, RECHAZADO
    private LocalDateTime fechaPago;

    public Pago(Long id, Long reservaId, BigDecimal monto, String metodoPago, String estadoPago, LocalDateTime fechaPago) {
        this.id = id;
        this.reservaId = reservaId;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.estadoPago = estadoPago != null ? estadoPago : "PENDIENTE";
        this.fechaPago = fechaPago != null ? fechaPago : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public BigDecimal getMonto() { return monto; }
    public String getMetodoPago() { return metodoPago; }
    public String getEstadoPago() { return estadoPago; }
    public LocalDateTime getFechaPago() { return fechaPago; }

    public void completar() { this.estadoPago = "COMPLETADO"; }
    public void rechazar() { this.estadoPago = "RECHAZADO"; }
}