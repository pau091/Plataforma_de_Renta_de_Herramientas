package com.example.tomcat.adapter.out.persistence.entity;

import com.example.tomcat.model.Garantia;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Columnas del depósito de garantía y su liquidación. Se embeben igual en reservas (el dato de negocio) y en
 * facturas (la copia que muestra el comprobante).
 */
@Embeddable
public class GarantiaEmbeddable {

    @Column(name = "deposito", nullable = false)
    private BigDecimal deposito = BigDecimal.ZERO;

    @Column(name = "estado_deposito")
    private String estado;

    @Column(name = "dias_retraso", nullable = false)
    private int diasRetraso;

    @Column(name = "multa_retraso", nullable = false)
    private BigDecimal multaRetraso = BigDecimal.ZERO;

    @Column(name = "cargo_danos", nullable = false)
    private BigDecimal cargoDanos = BigDecimal.ZERO;

    @Column(name = "deposito_devuelto", nullable = false)
    private BigDecimal montoDevuelto = BigDecimal.ZERO;

    @Column(name = "saldo_pendiente", nullable = false)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    public static GarantiaEmbeddable de(Garantia garantia) {
        GarantiaEmbeddable columnas = new GarantiaEmbeddable();
        Garantia g = garantia != null ? garantia : Garantia.sinCobrar();
        columnas.deposito = g.deposito();
        columnas.estado = g.estado();
        columnas.diasRetraso = (int) g.diasRetraso();
        columnas.multaRetraso = g.multaRetraso();
        columnas.cargoDanos = g.cargoDanos();
        columnas.montoDevuelto = g.montoDevuelto();
        columnas.saldoPendiente = g.saldoPendiente();
        return columnas;
    }

    public Garantia aDominio() {
        return new Garantia(deposito, estado, diasRetraso, multaRetraso, cargoDanos, montoDevuelto, saldoPendiente);
    }
}
