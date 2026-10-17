package com.example.tomcat.repository;

import com.example.tomcat.model.Pago;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepositoryPort {
    Pago guardar(Pago pago);
    Optional<Pago> buscarPorId(Long id);
    List<Pago> listar();
    List<Pago> listarPorReserva(Long reservaId);
    List<Pago> listarPorReservas(Collection<Long> reservaIds);
    boolean existePagoCompletado(Long reservaId);
}
