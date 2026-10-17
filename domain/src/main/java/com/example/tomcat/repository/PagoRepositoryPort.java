package com.example.tomcat.repository;

import com.example.tomcat.model.Pago;

import java.util.List;
import java.util.Optional;

public interface PagoRepositoryPort {
    Pago guardar(Pago pago);
    Optional<Pago> buscarPorId(Long id);
    List<Pago> listar();
    List<Pago> listarPorReserva(Long reservaId);
    boolean existePagoCompletado(Long reservaId);
}
