package com.example.tomcat.model;

import java.time.LocalDateTime;

public class Usuario {
    private Long id;
    private String nombre;
    private String email;
    private String password;
    private boolean activo;
    private LocalDateTime createdAt;
    private Rol rol;

    public Usuario(Long id, String nombre, String email, String password, boolean activo, LocalDateTime createdAt, Rol rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.activo = activo;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.rol = rol != null ? rol : Rol.CLIENTE;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Rol getRol() { return rol; }

    public void actualizarDatos(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    public void cambiarPassword(String passwordHash) {
        this.password = passwordHash;
    }

    public void cambiarRol(Rol rol) {
        this.rol = rol;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}
