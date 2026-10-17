package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.PagoJpaMapper;
import com.example.tomcat.adapter.out.persistence.entity.PagoEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataPagoRepository;
import com.example.tomcat.model.Pago;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PagoRepositoryAdapter implements PagoRepositoryPort {

    private final SpringDataPagoRepository repository;
    private final PagoJpaMapper mapper;

    public PagoRepositoryAdapter(SpringDataPagoRepository repository, PagoJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Pago guardar(Pago pago) {
        PagoEntity entity = pago.getId() == null
                ? new PagoEntity()
                : repository.findById(pago.getId()).orElse(new PagoEntity());
        mapper.copyToEntity(pago, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Pago> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Pago> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Pago> listarPorReserva(Long reservaId) {
        return repository.findByReservaId(reservaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePagoCompletado(Long reservaId) {
        return repository.existsByReservaIdAndEstadoPago(reservaId, "COMPLETADO");
    }
}
