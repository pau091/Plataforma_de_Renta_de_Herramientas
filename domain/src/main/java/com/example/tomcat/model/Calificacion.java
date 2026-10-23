package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.time.LocalDateTime;

/**
 * Reseña que deja el cliente al terminar un alquiler: califica la herramienta y/o al proveedor de 1 a 5 estrellas,
 * con un comentario opcional. Solo hay una por reserva y solo puede dejarla quien la alquiló (reserva FINALIZADA).
 */
public class Calificacion {

    private Long id;
    private Long reservaId;
    private Long clienteId;
    private String clienteNombre;
    private Long herramientaId;
    private Long proveedorId;
    private Integer estrellasHerramienta;
    private Integer estrellasProveedor;
    private String comentario;
    private LocalDateTime fecha;

    public Calificacion(Long id, Long reservaId, Long clienteId, String clienteNombre, Long herramientaId, Long proveedorId,
                        Integer estrellasHerramienta, Integer estrellasProveedor, String comentario, LocalDateTime fecha) {
        this.id = id;
        this.reservaId = reservaId;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.herramientaId = herramientaId;
        this.proveedorId = proveedorId;
        this.estrellasHerramienta = estrellasHerramienta;
        this.estrellasProveedor = estrellasProveedor;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    /** Crea la reseña de una reserva terminada; valida estrellas (1 a 5, al menos una) y el comentario. */
    public static Calificacion de(Reserva reserva, Herramienta herramienta, String clienteNombre, Integer estrellasHerramienta,
                                  Integer estrellasProveedor, String comentario, LocalDateTime ahora) {
        if (!"FINALIZADA".equals(reserva.getEstado())) {
            throw new DomainException("Solo se puede calificar un alquiler terminado (reserva FINALIZADA).");
        }
        if (estrellasHerramienta == null && estrellasProveedor == null) {
            throw new DomainException("Califique la herramienta, al proveedor o ambos (de 1 a 5 estrellas).");
        }
        validarEstrellas(estrellasHerramienta);
        validarEstrellas(estrellasProveedor);
        String texto = comentario == null || comentario.isBlank() ? null : comentario.trim();
        if (texto != null && texto.length() > 500) {
            throw new DomainException("El comentario admite máximo 500 caracteres.");
        }
        return new Calificacion(null, reserva.getId(), reserva.getClienteId(), clienteNombre, herramienta.getId(),
                herramienta.getProveedorId(), estrellasHerramienta, estrellasProveedor, texto, ahora);
    }

    private static void validarEstrellas(Integer estrellas) {
        if (estrellas != null && (estrellas < 1 || estrellas > 5)) {
            throw new DomainException("Las estrellas van de 1 a 5.");
        }
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public Long getHerramientaId() { return herramientaId; }
    public Long getProveedorId() { return proveedorId; }
    public Integer getEstrellasHerramienta() { return estrellasHerramienta; }
    public Integer getEstrellasProveedor() { return estrellasProveedor; }
    public String getComentario() { return comentario; }
    public LocalDateTime getFecha() { return fecha; }
}
