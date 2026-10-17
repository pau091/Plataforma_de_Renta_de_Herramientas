package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.ReservaJpaMapper;
import com.example.tomcat.adapter.out.persistence.entity.ReservaEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataHerramientaRepository;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataReservaRepository;
import com.example.tomcat.model.Reserva;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservaRepositoryAdapter implements ReservaRepositoryPort {

    private final SpringDataReservaRepository reservaRepository;
    private final SpringDataHerramientaRepository herramientaRepository;
    private final ReservaJpaMapper mapper;

    public ReservaRepositoryAdapter(
            SpringDataReservaRepository reservaRepository,
            SpringDataHerramientaRepository herramientaRepository,
            ReservaJpaMapper mapper
    ) {
        this.reservaRepository = reservaRepository;
        this.herramientaRepository = herramientaRepository;
        this.mapper = mapper;
    }

    @Override
    public Reserva guardar(Reserva reserva) {
        ReservaEntity entity = reserva.getId() == null
                ? new ReservaEntity()
                : reservaRepository.findById(reserva.getId()).orElse(new ReservaEntity());
        mapper.copyToEntity(reserva, entity);
        return mapper.toDomain(reservaRepository.save(entity));
    }

    @Override
    public Optional<Reserva> buscarPorId(Long id) {
        return reservaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Reserva> listar() {
        return reservaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reserva> listarPorCliente(Long clienteId) {
        return reservaRepository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reserva> listarPorProveedor(Long proveedorId) {
        List<Long> herramientaIds = herramientaRepository.findByProveedorId(proveedorId).stream()
                .map(entity -> entity.getId())
                .toList();
        if (herramientaIds.isEmpty()) {
            return List.of();
        }
        return reservaRepository.findByHerramientaIdIn(herramientaIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reserva> listarPorHerramienta(Long herramientaId) {
        return reservaRepository.findByHerramientaId(herramientaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reserva> listarPorEstados(Collection<String> estados) {
        return reservaRepository.findByEstadoIn(estados).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existeSolape(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, Long reservaIdExcluida) {
        return reservaRepository.existsSolape(herramientaId, fechaInicio, fechaFin, reservaIdExcluida);
    }
}
