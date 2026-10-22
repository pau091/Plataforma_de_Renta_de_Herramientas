package com.example.tomcat.port.in.dto;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.model.Rol;

/**
 * Usuario que ejecuta un caso de uso. Lo construye la capa de infraestructura a partir del JWT,
 * de modo que los servicios nunca confían en ids de cliente o proveedor enviados en la petición.
 */
public record Solicitante(Long id, Rol rol) {

    public Solicitante {
        if (id == null || rol == null) {
            throw new AccesoDenegadoException("No se pudo identificar al usuario autenticado.");
        }
    }

    public boolean esAdmin() { return rol == Rol.ADMIN; }
    public boolean esProveedor() { return rol == Rol.PROVEEDOR; }
    public boolean esCliente() { return rol == Rol.CLIENTE; }
    /** Puede reservar y pagar a su nombre: los clientes y también los proveedores (alquilando equipos de otros). */
    public boolean puedeAlquilar() { return esCliente() || esProveedor(); }

    public boolean esUsuario(Long usuarioId) {
        return id.equals(usuarioId);
    }
}
