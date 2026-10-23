package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Usuario {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private boolean activo;
    private LocalDateTime createdAt;
    private Rol rol;
    /** Fin de la suspensión (UTC). Mientras no llegue, la cuenta está bloqueada; después se libera sola. */
    private LocalDateTime suspendidoHasta;
    /** Baneo: bloqueo indefinido hasta que un administrador lo levante. */
    private boolean baneado;
    private String motivoSancion;

    public Usuario(Long id, String nombre, String email, String password, boolean activo, LocalDateTime createdAt, Rol rol) {
        this(id, nombre, email, password, activo, createdAt, rol, null, false, null);
    }

    public Usuario(Long id, String nombre, String email, String password, boolean activo, LocalDateTime createdAt, Rol rol,
                   LocalDateTime suspendidoHasta, boolean baneado, String motivoSancion) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.activo = activo;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.rol = rol != null ? rol : Rol.CLIENTE;
        this.suspendidoHasta = suspendidoHasta;
        this.baneado = baneado;
        this.motivoSancion = motivoSancion;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Rol getRol() { return rol; }
    public LocalDateTime getSuspendidoHasta() { return suspendidoHasta; }
    public boolean isBaneado() { return baneado; }
    public String getMotivoSancion() { return motivoSancion; }

    public void actualizarDatos(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    public void cambiarPassword(String passwordHash) {
        this.password = passwordHash;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void cambiarRol(Rol nuevoRol) {
        this.rol = nuevoRol;
    }

    // ------------------------------------------------------------------ sanciones

    /** Suspensión temporal: la cuenta queda bloqueada hasta {@code hasta} (UTC) y luego se libera sola. */
    public void suspender(LocalDateTime hasta, String motivo, LocalDateTime ahoraUtc) {
        if (baneado) {
            throw new DomainException("El usuario está baneado. Reactívelo antes de suspenderlo.");
        }
        if (hasta == null || !hasta.isAfter(ahoraUtc)) {
            throw new DomainException("La suspensión debe terminar en una fecha futura.");
        }
        this.suspendidoHasta = hasta;
        this.motivoSancion = exigirMotivo(motivo);
    }

    /** Baneo: bloqueo indefinido. */
    public void banear(String motivo) {
        if (baneado) {
            throw new DomainException("El usuario ya está baneado.");
        }
        this.baneado = true;
        this.suspendidoHasta = null;
        this.motivoSancion = exigirMotivo(motivo);
    }

    /** Levanta la suspensión o el baneo. Los tokens revocados siguen sin servir: debe iniciar sesión de nuevo. */
    public void levantarSancion(LocalDateTime ahoraUtc) {
        if (!estaSancionado(ahoraUtc)) {
            throw new DomainException("El usuario no tiene una suspensión ni un baneo vigente.");
        }
        this.baneado = false;
        this.suspendidoHasta = null;
        this.motivoSancion = null;
    }

    public boolean estaSuspendido(LocalDateTime ahoraUtc) {
        return suspendidoHasta != null && suspendidoHasta.isAfter(ahoraUtc);
    }

    /** Baneado o con una suspensión vigente: no puede iniciar sesión ni usar la API. */
    public boolean estaSancionado(LocalDateTime ahoraUtc) {
        return baneado || estaSuspendido(ahoraUtc);
    }

    /** ACTIVA, INACTIVA (desactivada por el admin), SUSPENDIDA o BANEADA. */
    public String estadoCuenta(LocalDateTime ahoraUtc) {
        if (baneado) {
            return "BANEADA";
        }
        if (estaSuspendido(ahoraUtc)) {
            return "SUSPENDIDA";
        }
        return activo ? "ACTIVA" : "INACTIVA";
    }

    /** Mensaje para el usuario sancionado; la fecha se muestra en la zona horaria del negocio. */
    public String mensajeSancion(LocalDateTime ahoraUtc, ZoneId zona) {
        if (baneado) {
            return "Su cuenta fue baneada. Motivo: " + motivoSancion + ". Contacte al administrador de la plataforma.";
        }
        if (estaSuspendido(ahoraUtc)) {
            String hasta = suspendidoHasta.atOffset(ZoneOffset.UTC).atZoneSameInstant(zona).format(FECHA_HORA);
            return "Su cuenta está suspendida hasta el " + hasta + ". Motivo: " + motivoSancion + ".";
        }
        return null;
    }

    private static String exigirMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new DomainException("Debe indicar el motivo de la sanción.");
        }
        String limpio = motivo.trim();
        if (limpio.length() > 255) {
            throw new DomainException("El motivo admite máximo 255 caracteres.");
        }
        return limpio;
    }
}
