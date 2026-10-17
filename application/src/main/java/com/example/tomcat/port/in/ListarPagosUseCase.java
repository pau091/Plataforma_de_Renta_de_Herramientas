package com.example.tomcat.port.in;

import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.dto.Solicitante;

import java.util.List;

public interface ListarPagosUseCase {
    /** ADMIN ve todos; PROVEEDOR los de reservas de sus herramientas; CLIENTE los de sus reservas. */
    List<Pago> listar(Solicitante solicitante);
    List<Pago> historialPorReserva(Solicitante solicitante, Long reservaId);
    Pago buscarPorId(Solicitante solicitante, Long id);
}
