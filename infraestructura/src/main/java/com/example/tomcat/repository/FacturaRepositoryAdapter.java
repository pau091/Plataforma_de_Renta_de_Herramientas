package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.FacturaJpaMapper;
import com.example.tomcat.adapter.out.persistence.entity.FacturaEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataFacturaRepository;
import com.example.tomcat.model.Factura;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class FacturaRepositoryAdapter implements FacturaRepositoryPort {

    private final SpringDataFacturaRepository repository;
    private final FacturaJpaMapper mapper;

    public FacturaRepositoryAdapter(SpringDataFacturaRepository repository, FacturaJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Factura guardar(Factura factura) {
        FacturaEntity entity = factura.getId() == null
                ? new FacturaEntity()
                : repository.findById(factura.getId()).orElse(new FacturaEntity());
        mapper.copyToEntity(factura, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Factura> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Factura> buscarPorPagoId(Long pagoId) {
        return repository.findByPagoId(pagoId).map(mapper::toDomain);
    }

    @Override
    public Optional<Factura> buscarPorReservaId(Long reservaId) {
        return repository.findByReservaId(reservaId).map(mapper::toDomain);
    }

    @Override
    public List<Factura> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Factura> listarPorCliente(Long clienteId) {
        return repository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Factura> listarPorHerramientas(Collection<Long> herramientaIds) {
        if (herramientaIds.isEmpty()) {
            return List.of();
        }
        return repository.findByHerramientaIdIn(herramientaIds).stream().map(mapper::toDomain).toList();
    }
}
