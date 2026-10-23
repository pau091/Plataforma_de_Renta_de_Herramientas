package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.ListaEsperaJpaMapper;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataNotificacionRepository;
import com.example.tomcat.model.Notificacion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NotificacionRepositoryAdapter implements NotificacionRepositoryPort {

    private final SpringDataNotificacionRepository repository;
    private final ListaEsperaJpaMapper mapper;

    public NotificacionRepositoryAdapter(SpringDataNotificacionRepository repository, ListaEsperaJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        return mapper.toDomain(repository.save(mapper.toEntity(notificacion)));
    }

    @Override
    public Optional<Notificacion> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioIdOrderByCreadaEnDescIdDesc(usuarioId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long contarNoLeidas(Long usuarioId) {
        return repository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Override
    public void marcarTodasLeidas(Long usuarioId) {
        repository.marcarTodasLeidas(usuarioId);
    }
}
