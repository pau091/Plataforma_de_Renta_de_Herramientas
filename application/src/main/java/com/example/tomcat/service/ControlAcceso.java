package com.example.tomcat.service;

import com.example.tomcat.exception.AccesoDenegadoException;
import com.example.tomcat.model.Factura;
import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.dto.Solicitante;
import com.example.tomcat.repository.HerramientaRepositoryPort;

import java.util.List;

/**
 * Reglas de propiedad compartidas por los servicios:
 * - ADMIN accede a todo.
 * - PROVEEDOR solo gestiona sus herramientas y lo relacionado con ellas (reservas, pagos, facturas, mantenimientos).
 * - CLIENTE solo accede a sus propias reservas, pagos y facturas.
 */
public class ControlAcceso {

    private final HerramientaRepositoryPort herramientaRepositoryPort;

    public ControlAcceso(HerramientaRepositoryPort herramientaRepositoryPort) {
        this.herramientaRepositoryPort = herramientaRepositoryPort;
    }

    public void verificarPropietarioHerramienta(Solicitante solicitante, Herramienta herramienta) {
        if (solicitante.esAdmin()) {
            return;
        }
        if (solicitante.esProveedor() && solicitante.esUsuario(herramienta.getProveedorId())) {
            return;
        }
        throw new AccesoDenegadoException("Solo el proveedor dueño de la herramienta puede gestionarla.");
    }

    /** Lectura de una reserva: el cliente que la hizo, el proveedor de la herramienta o un admin. */
    public void verificarAccesoReserva(Solicitante solicitante, Reserva reserva) {
        if (solicitante.esAdmin()) {
            return;
        }
        if (solicitante.esCliente() && solicitante.esUsuario(reserva.getClienteId())) {
            return;
        }
        if (solicitante.esProveedor() && esPropietario(solicitante, reserva.getHerramientaId())) {
            return;
        }
        throw new AccesoDenegadoException("No tiene permiso para acceder a esta reserva.");
    }

    /** Aceptar, rechazar, entregar o recibir devolución: solo el proveedor de la herramienta o un admin. */
    public void verificarGestionReserva(Solicitante solicitante, Reserva reserva) {
        if (solicitante.esAdmin()) {
            return;
        }
        if (solicitante.esProveedor() && esPropietario(solicitante, reserva.getHerramientaId())) {
            return;
        }
        throw new AccesoDenegadoException("Solo el proveedor dueño de la herramienta puede gestionar esta reserva.");
    }

    public void verificarAccesoFactura(Solicitante solicitante, Factura factura) {
        if (solicitante.esAdmin()) {
            return;
        }
        if (solicitante.esCliente() && solicitante.esUsuario(factura.getClienteId())) {
            return;
        }
        if (solicitante.esProveedor() && esPropietario(solicitante, factura.getHerramientaId())) {
            return;
        }
        throw new AccesoDenegadoException("No tiene permiso para acceder a esta factura.");
    }

    public List<Long> idsHerramientasDelProveedor(Long proveedorId) {
        return herramientaRepositoryPort.listarPorProveedor(proveedorId).stream()
                .map(Herramienta::getId)
                .toList();
    }

    private boolean esPropietario(Solicitante solicitante, Long herramientaId) {
        return herramientaRepositoryPort.buscarPorId(herramientaId)
                .map(herramienta -> solicitante.esUsuario(herramienta.getProveedorId()))
                .orElse(false);
    }
}
