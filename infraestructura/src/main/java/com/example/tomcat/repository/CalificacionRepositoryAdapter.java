package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.CalificacionJpaMapper;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataCalificacionRepository;
import com.example.tomcat.model.Calificacion;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CalificacionRepositoryAdapter implements CalificacionRepositoryPort {

    private final SpringDataCalificacionRepository repository;
    private final CalificacionJpaMapper mapper;

    public CalificacionRepositoryAdapter(SpringDataCalificacionRepository repository, CalificacionJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Calificacion guardar(Calificacion calificacion) {
        return mapper.toDomain(repository.save(mapper.toEntity(calificacion)));
    }

    @Override
    public boolean existePorReserva(Long reservaId) {
        return repository.existsByReservaId(reservaId);
    }

    @Override
    public List<Calificacion> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Calificacion> listarPorHerramienta(Long herramientaId) {
        return repository.findByHerramientaIdOrderByFechaDesc(herramientaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Calificacion> listarPorCliente(Long clienteId) {
        return repository.findByClienteIdOrderByFechaDesc(clienteId).stream().map(mapper::toDomain).toList();
    }
}
