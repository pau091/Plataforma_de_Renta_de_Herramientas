package com.example.tomcat.port.in;

import com.example.tomcat.model.Categoria;

import java.util.List;

public interface GestionarCategoriaUseCase {
    List<Categoria> listar();
    Categoria buscarPorId(Long id);
    Categoria crear(String nombre, String descripcion);
    /** nombre y descripcion son opcionales: solo se cambia lo que se envía. */
    Categoria actualizar(Long id, String nombre, String descripcion);
    /** Solo se eliminan categorías que no tengan herramientas asociadas. */
    void eliminar(Long id);
}
