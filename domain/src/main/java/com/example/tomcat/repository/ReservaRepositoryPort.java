package com.example.tomcat.repository;

import com.example.tomcat.model.Reserva;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepositoryPort {
    Reserva guardar(Reserva reserva);
    Optional<Reserva> buscarPorId(Long id);
    List<Reserva> listar();
    List<Reserva> listarPorCliente(Long clienteId);
    List<Reserva> listarPorProveedor(Long proveedorId);
    List<Reserva> listarPorHerramienta(Long herramientaId);
    /** Reservas en alguno de los estados indicados cuya fecha de inicio es anterior a {@code fecha}. */
    List<Reserva> listarPorEstadosConInicioAntesDe(Collection<String> estados, LocalDate fecha);
    boolean existeSolape(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, Long reservaIdExcluida);
}
