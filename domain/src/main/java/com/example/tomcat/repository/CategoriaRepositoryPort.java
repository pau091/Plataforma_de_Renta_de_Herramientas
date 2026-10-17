package com.example.tomcat.repository;

import com.example.tomcat.model.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepositoryPort {
    Categoria guardar(Categoria categoria);
    List<Categoria> listar();
    Optional<Categoria> buscarPorId(Long id);
}
