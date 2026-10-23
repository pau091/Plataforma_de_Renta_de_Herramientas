package com.example.tomcat.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class ReservaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "herramienta_id", nullable = false)
    private Long herramientaId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;

    @Column(name = "estado_devolucion")
    private String estadoDevolucion;

    @Column(name = "observaciones_devolucion")
    private String observacionesDevolucion;

    /** Instante de creación en UTC: desde aquí corre el plazo del proveedor para aceptarla. */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime fechaCreacion;

    /** Instante de aceptación en UTC: desde aquí corre el plazo del cliente para pagarla. */
    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    /**
     * Bloqueo optimista: cada UPDATE exige que la versión no haya cambiado desde que se leyó la reserva. Si otra
     * transacción la modificó antes (dos pagos simultáneos, pago contra vencimiento...), falla en vez de pisarla.
     */
    @Version
    private Long version;

    @Embedded
    private GarantiaEmbeddable garantia = new GarantiaEmbeddable();

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public GarantiaEmbeddable getGarantia() { return garantia; }
    public void setGarantia(GarantiaEmbeddable garantia) { this.garantia = garantia; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public Long getHerramientaId() { return herramientaId; }
    public void setHerramientaId(Long herramientaId) { this.herramientaId = herramientaId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public LocalDateTime getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDateTime fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }
    public String getEstadoDevolucion() { return estadoDevolucion; }
    public void setEstadoDevolucion(String estadoDevolucion) { this.estadoDevolucion = estadoDevolucion; }
    public String getObservacionesDevolucion() { return observacionesDevolucion; }
    public void setObservacionesDevolucion(String observacionesDevolucion) { this.observacionesDevolucion = observacionesDevolucion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaAceptacion() { return fechaAceptacion; }
    public void setFechaAceptacion(LocalDateTime fechaAceptacion) { this.fechaAceptacion = fechaAceptacion; }
}
