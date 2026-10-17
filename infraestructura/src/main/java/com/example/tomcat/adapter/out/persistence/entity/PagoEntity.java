package com.example.tomcat.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    @Column(name = "estado_pago", nullable = false)
    private String estadoPago;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}
