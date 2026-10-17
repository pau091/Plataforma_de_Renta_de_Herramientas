package com.example.tomcat.repository;

import com.example.tomcat.adapter.out.mapper.UsuarioJpaMapper;
import com.example.tomcat.adapter.out.persistence.entity.RolEntity;
import com.example.tomcat.adapter.out.persistence.entity.UsuarioEntity;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataRolRepository;
import com.example.tomcat.adapter.out.persistence.repository.SpringDataUsuarioRepository;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final SpringDataUsuarioRepository usuarioRepository;
    private final SpringDataRolRepository rolRepository;
    private final UsuarioJpaMapper mapper;

    public UsuarioRepositoryAdapter(
            SpringDataUsuarioRepository usuarioRepository,
            SpringDataRolRepository rolRepository,
            UsuarioJpaMapper mapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.mapper = mapper;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = usuario.getId() == null
                ? new UsuarioEntity()
                : usuarioRepository.findById(usuario.getId()).orElse(new UsuarioEntity());

        mapper.copyToEntity(usuario, entity);

        RolEntity rolEntity = rolRepository.findByNombre(usuario.getRol().name())
                .orElseThrow(() -> new DomainException("El rol " + usuario.getRol() + " no existe en la base de datos."));

        Set<RolEntity> roles = new HashSet<>();
        roles.add(rolEntity);
        entity.setRoles(roles);

        UsuarioEntity saved = usuarioRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Usuario> listar() {
        return usuarioRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Usuario> listarPorRol(Rol rol) {
        return usuarioRepository.findByRolNombre(rol.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }
}
