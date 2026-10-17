package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.MantenimientoJpaMapper;
import com.example.tomcat.adapter.out.persistence.entity.MantenimientoEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataMantenimientoRepository;
import com.example.tomcat.model.Mantenimiento;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MantenimientoRepositoryAdapter implements MantenimientoRepositoryPort {

    private final SpringDataMantenimientoRepository repository;
    private final MantenimientoJpaMapper mapper;

    public MantenimientoRepositoryAdapter(SpringDataMantenimientoRepository repository, MantenimientoJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mantenimiento guardar(Mantenimiento mantenimiento) {
        MantenimientoEntity entity = mantenimiento.getId() == null
                ? new MantenimientoEntity()
                : repository.findById(mantenimiento.getId()).orElse(new MantenimientoEntity());
        mapper.copyToEntity(mantenimiento, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Mantenimiento> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Mantenimiento> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Mantenimiento> listarPorHerramienta(Long herramientaId) {
        return repository.findByHerramientaId(herramientaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Mantenimiento> listarPorReserva(Long reservaId) {
        return repository.findByReservaId(reservaId).stream().map(mapper::toDomain).toList();
    }
}
