package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public class Pago {
    private static final Set<String> METODOS = Set.of("TARJETA", "TRANSFERENCIA", "EFECTIVO");

    private Long id;
    private Long reservaId;
    private BigDecimal monto;
    private String metodoPago; // TARJETA, TRANSFERENCIA, EFECTIVO
    private String estadoPago; // PENDIENTE, COMPLETADO, RECHAZADO, REEMBOLSADO
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

    public boolean estaCompletado() {
        return "COMPLETADO".equals(estadoPago);
    }

    /** Normaliza y valida el método de pago (TARJETA, TRANSFERENCIA o EFECTIVO). */
    public static String normalizarMetodo(String metodoPago) {
        String metodo = metodoPago == null ? "" : metodoPago.trim().toUpperCase();
        if (!METODOS.contains(metodo)) {
            throw new DomainException("Método de pago no válido. Use TARJETA, TRANSFERENCIA o EFECTIVO.");
        }
        return metodo;
    }

    /** Corrige el método con el que se registró el pago (el monto no se modifica: lo fija la tarifa). */
    public void cambiarMetodo(String nuevoMetodo) {
        exigirCompletado("Solo se puede corregir el método de pagos completados.");
        this.metodoPago = normalizarMetodo(nuevoMetodo);
    }

    /** Anula el pago devolviendo el dinero al cliente. Deja de contar como ingreso. */
    public void reembolsar() {
        exigirCompletado("Solo se pueden reembolsar pagos completados.");
        this.estadoPago = "REEMBOLSADO";
    }

    private void exigirCompletado(String mensaje) {
        if (!estaCompletado()) {
            throw new DomainException(mensaje);
        }
    }
}
