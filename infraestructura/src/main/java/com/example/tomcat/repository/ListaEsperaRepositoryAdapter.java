package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.ListaEsperaJpaMapper;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataListaEsperaRepository;
import com.example.tomcat.model.EntradaListaEspera;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ListaEsperaRepositoryAdapter implements ListaEsperaRepositoryPort {

    private static final List<String> ACTIVAS = List.of("ESPERANDO", "NOTIFICADA");

    private final SpringDataListaEsperaRepository repository;
    private final ListaEsperaJpaMapper mapper;

    public ListaEsperaRepositoryAdapter(SpringDataListaEsperaRepository repository, ListaEsperaJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public EntradaListaEspera guardar(EntradaListaEspera entrada) {
        return mapper.toDomain(repository.save(mapper.toEntity(entrada)));
    }

    @Override
    public Optional<EntradaListaEspera> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<EntradaListaEspera> listarPorCliente(Long clienteId) {
        return repository.findByClienteIdOrderByCreadaEnDesc(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<EntradaListaEspera> listarActivasPorHerramienta(Long herramientaId) {
        return repository.findByHerramientaIdAndEstadoInOrderByCreadaEnAscIdAsc(herramientaId, ACTIVAS).stream()
                .map(mapper::toDomain).toList();
    }
}
