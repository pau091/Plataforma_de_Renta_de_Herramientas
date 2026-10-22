package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.HerramientaJpaMapper;
import com.example.tomcat.adapter.out.persistence.entity.HerramientaEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataHerramientaRepository;
import com.example.tomcat.model.Herramienta;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HerramientaRepositoryAdapter implements HerramientaRepositoryPort {

    private final SpringDataHerramientaRepository repository;
    private final HerramientaJpaMapper mapper;

    public HerramientaRepositoryAdapter(SpringDataHerramientaRepository repository, HerramientaJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Herramienta guardar(Herramienta herramienta) {
        HerramientaEntity entity = herramienta.getId() == null
                ? new HerramientaEntity()
                : repository.findById(herramienta.getId()).orElse(new HerramientaEntity());
        mapper.copyToEntity(herramienta, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<Herramienta> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Herramienta> buscar(Long categoriaId, Long proveedorId, String estado, String texto) {
        String textoNormalizado = texto == null || texto.isBlank() ? null : texto.trim().toLowerCase();
        return repository.buscar(categoriaId, proveedorId, estado, textoNormalizado).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Herramienta> listarDisponibles() {
        return repository.findByEstado("DISPONIBLE").stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Herramienta> listarPorProveedor(Long proveedorId) {
        return repository.findByProveedorId(proveedorId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Herramienta> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Herramienta> bloquearPorId(Long id) {
        return repository.findByIdParaActualizar(id).map(mapper::toDomain);
    }

    @Override
    public boolean existePorCategoria(Long categoriaId) {
        return repository.existsByCategoriaId(categoriaId);
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
