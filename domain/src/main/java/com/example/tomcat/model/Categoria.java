package com.example.tomcat.model;

public class Categoria {
    private Long id;
    private String nombre;
    private String descripcion;

    public Categoria(Long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
