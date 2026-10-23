package com.example.tomcat.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "calificaciones")
public class CalificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Única: una reseña por reserva (también lo garantiza la base de datos). */
    @Column(name = "reserva_id", nullable = false, unique = true)
    private Long reservaId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "cliente_nombre")
    private String clienteNombre;

    @Column(name = "herramienta_id", nullable = false)
    private Long herramientaId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "estrellas_herramienta")
    private Integer estrellasHerramienta;

    @Column(name = "estrellas_proveedor")
    private Integer estrellasProveedor;

    @Column(length = 500)
    private String comentario;

    /** Instante en UTC. */
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public Long getHerramientaId() { return herramientaId; }
    public void setHerramientaId(Long herramientaId) { this.herramientaId = herramientaId; }
    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public Integer getEstrellasHerramienta() { return estrellasHerramienta; }
    public void setEstrellasHerramienta(Integer estrellasHerramienta) { this.estrellasHerramienta = estrellasHerramienta; }
    public Integer getEstrellasProveedor() { return estrellasProveedor; }
    public void setEstrellasProveedor(Integer estrellasProveedor) { this.estrellasProveedor = estrellasProveedor; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
