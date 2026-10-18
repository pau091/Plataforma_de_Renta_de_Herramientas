package com.example.tomcat.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Comprobante generado automáticamente al pagar una reserva. Queda ANULADA si el pago se reembolsa. */
public class Factura {
    private Long id;
    private String numero;
    private Long pagoId;
    private Long reservaId;
    private Long clienteId;
    private String clienteNombre;
    private String clienteEmail;
    private Long herramientaId;
    private String herramientaNombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private long dias;
    private BigDecimal tarifaDiaria;
    private BigDecimal subtotal;
    private BigDecimal total;
    private String metodoPago;
    private LocalDateTime fechaEmision;
    private String estado; // VIGENTE, ANULADA

    public Factura(Long id, String numero, Long pagoId, Long reservaId, Long clienteId, String clienteNombre,
                   String clienteEmail, Long herramientaId, String herramientaNombre, LocalDate fechaInicio,
                   LocalDate fechaFin, long dias, BigDecimal tarifaDiaria, BigDecimal subtotal, BigDecimal total,
                   String metodoPago, LocalDateTime fechaEmision, String estado) {
        this.id = id;
        this.numero = numero;
        this.pagoId = pagoId;
        this.reservaId = reservaId;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.herramientaId = herramientaId;
        this.herramientaNombre = herramientaNombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.dias = dias;
        this.tarifaDiaria = tarifaDiaria;
        this.subtotal = subtotal;
        this.total = total;
        this.metodoPago = metodoPago;
        this.fechaEmision = fechaEmision != null ? fechaEmision : LocalDateTime.now();
        this.estado = estado != null ? estado : "VIGENTE";
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Long getPagoId() { return pagoId; }
    public Long getReservaId() { return reservaId; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteEmail() { return clienteEmail; }
    public Long getHerramientaId() { return herramientaId; }
    public String getHerramientaNombre() { return herramientaNombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public long getDias() { return dias; }
    public BigDecimal getTarifaDiaria() { return tarifaDiaria; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTotal() { return total; }
    public String getMetodoPago() { return metodoPago; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public String getEstado() { return estado; }

    public boolean estaAnulada() {
        return "ANULADA".equals(estado);
    }

    /** Refleja la corrección del método en el pago asociado. */
    public void cambiarMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public void anular() {
        this.estado = "ANULADA";
    }
}
