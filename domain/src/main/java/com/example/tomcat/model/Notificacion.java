package com.example.tomcat.model;

import java.time.LocalDateTime;

/** Aviso que el usuario ve en la campana de la página (por ejemplo, que se liberaron las fechas que esperaba). */
public class Notificacion {

    private Long id;
    private Long usuarioId;
    private String tipo;
    private String titulo;
    private String mensaje;
    private Long herramientaId;
    private boolean leida;
    private LocalDateTime creadaEn;

    public Notificacion(Long id, Long usuarioId, String tipo, String titulo, String mensaje, Long herramientaId,
                        boolean leida, LocalDateTime creadaEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.herramientaId = herramientaId;
        this.leida = leida;
        this.creadaEn = creadaEn;
    }

    public static Notificacion nueva(Long usuarioId, String tipo, String titulo, String mensaje, Long herramientaId,
                                     LocalDateTime ahoraUtc) {
        return new Notificacion(null, usuarioId, tipo, titulo, mensaje, herramientaId, false, ahoraUtc);
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public Long getHerramientaId() { return herramientaId; }
    public boolean isLeida() { return leida; }
    public LocalDateTime getCreadaEn() { return creadaEn; }

    public void marcarLeida() {
        this.leida = true;
    }
}
