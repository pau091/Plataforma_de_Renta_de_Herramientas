package com.example.tomcat.repository;

import com.example.tomcat.model.Rol;
import com.example.tomcat.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {
    Usuario guardar(Usuario usuario);
    Optional<Usuario> buscarPorEmail(String email);
    Optional<Usuario> buscarPorId(Long id);
    List<Usuario> listar();
    List<Usuario> listarPorRol(Rol rol);
    void eliminar(Long id);
}
