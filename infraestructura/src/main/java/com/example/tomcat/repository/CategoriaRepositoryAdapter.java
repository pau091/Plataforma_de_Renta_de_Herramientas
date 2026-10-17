package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.CategoriaJpaMapper;
import com.example.tomcat.adapter.out.persistence.entity.CategoriaEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataCategoriaRepository;
import com.example.tomcat.model.Categoria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoriaRepositoryAdapter implements CategoriaRepositoryPort {

    private final SpringDataCategoriaRepository repository;
    private final CategoriaJpaMapper mapper;

    public CategoriaRepositoryAdapter(SpringDataCategoriaRepository repository, CategoriaJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Categoria guardar(Categoria categoria) {
        CategoriaEntity entity = categoria.getId() == null
                ? new CategoriaEntity()
                : repository.findById(categoria.getId()).orElse(new CategoriaEntity());
        entity.setNombre(categoria.getNombre());
        entity.setDescripcion(categoria.getDescripcion());
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<Categoria> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
