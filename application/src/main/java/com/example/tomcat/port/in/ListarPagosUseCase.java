package com.example.tomcat.port.in;

import com.example.tomcat.model.Pago;

import java.util.List;

public interface ListarPagosUseCase {
    List<Pago> listar();
    List<Pago> historialPorReserva(Long reservaId);
    Pago buscarPorId(Long id);
}
