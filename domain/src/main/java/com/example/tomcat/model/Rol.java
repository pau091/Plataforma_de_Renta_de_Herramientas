package com.example.tomcat.model;

import com.example.tomcat.exception.DomainException;

public enum Rol {
    ADMIN,
    PROVEEDOR,
    CLIENTE;

    public static Rol desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DomainException("El rol es obligatorio.");
        }
        try {
            return Rol.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Rol no válido. Use ADMIN, PROVEEDOR o CLIENTE.");
        }
    }
}
