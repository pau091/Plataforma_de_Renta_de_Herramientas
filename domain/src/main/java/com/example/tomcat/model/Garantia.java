package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.math.BigDecimal;

/**
 * Depósito de garantía de una reserva y su liquidación al devolver la herramienta (objeto de valor inmutable).
 *
 * <p>Se cobra junto con el alquiler y queda RETENIDO mientras la herramienta está fuera. Al devolverla se descuentan
 * los cargos: la multa por retraso y, si vuelve DAÑADA, el costo de reparación (que se conoce al cerrar el
 * mantenimiento; mientras tanto queda PENDIENTE_REPARACION). Si se PIERDE, el depósito no se devuelve.
 * Lo que sobra se le devuelve al cliente; si los cargos superan el depósito, la diferencia queda como saldo pendiente.
 *
 * <p>Estados: null = aún no se paga. Mientras la herramienta está fuera: RETENIDO, o SIN_DEPOSITO si la herramienta no
 * exige depósito. Al devolver: PENDIENTE_REPARACION (daño sin costo aún), DEVUELTO (completo), DEVUELTO_PARCIAL,
 * NO_DEVUELTO, o LIQUIDADO si no había depósito (solo se registran los cargos). REEMBOLSADO: se devolvió con el pago.
 */
public record Garantia(BigDecimal deposito, String estado, long diasRetraso, BigDecimal multaRetraso,
                       BigDecimal cargoDanos, BigDecimal montoDevuelto, BigDecimal saldoPendiente) {

    public Garantia {
        deposito = cero(deposito);
        multaRetraso = cero(multaRetraso);
        cargoDanos = cero(cargoDanos);
        montoDevuelto = cero(montoDevuelto);
        saldoPendiente = cero(saldoPendiente);
    }

    /** Reserva aún sin pagar. */
    public static Garantia sinCobrar() {
        return new Garantia(BigDecimal.ZERO, null, 0, null, null, null, null);
    }

    /** Al pagar: el depósito queda retenido mientras dure el alquiler. */
    public static Garantia retenida(BigDecimal deposito) {
        BigDecimal monto = cero(deposito);
        if (monto.signum() < 0) {
            throw new DomainException("El depósito de garantía no puede ser negativo.");
        }
        return new Garantia(monto, monto.signum() == 0 ? "SIN_DEPOSITO" : "RETENIDO", 0, null, null, null, null);
    }

    /**
     * Liquidación al devolver. BUENO: se descuenta solo la multa. PERDIDO: el depósito se pierde entero como
     * compensación (y la multa, si la hay, queda como saldo). DANADO: queda pendiente del costo de reparación.
     */
    public Garantia liquidarDevolucion(String estadoEquipo, long dias, BigDecimal multa) {
        exigirAbierta();
        return switch (estadoEquipo) {
            case "BUENO" -> liquidar(dias, multa, BigDecimal.ZERO);
            case "PERDIDO" -> liquidar(dias, multa, deposito);
            case "DANADO" -> new Garantia(deposito, "PENDIENTE_REPARACION", dias, multa, null, null, null);
            default -> throw new DomainException("Estado de devolución no válido. Use BUENO, DANADO o PERDIDO.");
        };
    }

    /** Al cerrar el mantenimiento de una devolución con daño: se descuenta el costo real de la reparación. */
    public Garantia liquidarReparacion(BigDecimal costoReparacion) {
        if (!pendienteReparacion()) {
            throw new DomainException("El depósito no está pendiente de una reparación.");
        }
        return liquidar(diasRetraso, multaRetraso, cero(costoReparacion));
    }

    /** El administrador reembolsó el pago antes de entregar la herramienta: el depósito se devuelve con él. */
    public Garantia reembolsada() {
        return new Garantia(deposito, "REEMBOLSADO", 0, null, null, deposito, null);
    }

    public boolean pendienteReparacion() {
        return "PENDIENTE_REPARACION".equals(estado);
    }

    /** Depósito que se aplicó a la multa y a los daños (no se le devolvió al cliente). */
    public BigDecimal depositoRetenido() {
        return liquidada() && !"REEMBOLSADO".equals(estado) ? deposito.subtract(montoDevuelto) : BigDecimal.ZERO;
    }

    /** Ya se liquidó (al devolver o por reembolso): se conoce lo devuelto y el saldo. */
    public boolean liquidada() {
        return "DEVUELTO".equals(estado) || "DEVUELTO_PARCIAL".equals(estado) || "NO_DEVUELTO".equals(estado)
                || "LIQUIDADO".equals(estado) || "REEMBOLSADO".equals(estado);
    }

    /** Depósito cobrado que todavía no se liquida (la herramienta está fuera o espera el costo de reparación). */
    public boolean enCustodia() {
        return "RETENIDO".equals(estado) || "PENDIENTE_REPARACION".equals(estado);
    }

    private Garantia liquidar(long dias, BigDecimal multa, BigDecimal danos) {
        BigDecimal cargos = cero(multa).add(cero(danos));
        BigDecimal devuelto = deposito.subtract(cargos).max(BigDecimal.ZERO);
        BigDecimal saldo = cargos.subtract(deposito).max(BigDecimal.ZERO);
        String nuevoEstado;
        if (deposito.signum() == 0) {
            nuevoEstado = "LIQUIDADO";
        } else if (devuelto.compareTo(deposito) == 0) {
            nuevoEstado = "DEVUELTO";
        } else if (devuelto.signum() == 0) {
            nuevoEstado = "NO_DEVUELTO";
        } else {
            nuevoEstado = "DEVUELTO_PARCIAL";
        }
        return new Garantia(deposito, nuevoEstado, dias, multa, danos, devuelto, saldo);
    }

    private void exigirAbierta() {
        if (!"RETENIDO".equals(estado) && !"SIN_DEPOSITO".equals(estado)) {
            throw new DomainException("La reserva no tiene un depósito pagado pendiente de liquidar.");
        }
    }

    private static BigDecimal cero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
