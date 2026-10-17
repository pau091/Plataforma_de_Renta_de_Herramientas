package com.example.tomcat.service;

import com.example.tomcat.exception.ConflictoException;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.exception.RecursoNoEncontradoException;
import com.example.tomcat.model.Categoria;
import com.example.tomcat.port.in.GestionarCategoriaUseCase;
import com.example.tomcat.repository.CategoriaRepositoryPort;
import com.example.tomcat.repository.HerramientaRepositoryPort;

import java.util.List;

public class GestionarCategoriaService implements GestionarCategoriaUseCase {

    private final CategoriaRepositoryPort categoriaRepositoryPort;
    private final HerramientaRepositoryPort herramientaRepositoryPort;

    public GestionarCategoriaService(CategoriaRepositoryPort categoriaRepositoryPort,
                                     HerramientaRepositoryPort herramientaRepositoryPort) {
        this.categoriaRepositoryPort = categoriaRepositoryPort;
        this.herramientaRepositoryPort = herramientaRepositoryPort;
    }

    @Override
    public List<Categoria> listar() {
        return categoriaRepositoryPort.listar();
    }

    @Override
    public Categoria buscarPorId(Long id) {
        return categoriaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe."));
    }

    @Override
    public Categoria crear(String nombre, String descripcion) {
        String nombreLimpio = validarNombre(nombre);
        exigirNombreLibre(nombreLimpio, null);
        return categoriaRepositoryPort.guardar(new Categoria(null, nombreLimpio, limpiar(descripcion)));
    }

    @Override
    public Categoria actualizar(Long id, String nombre, String descripcion) {
        Categoria categoria = buscarPorId(id);
        String nombreFinal = categoria.getNombre();
        if (nombre != null && !nombre.isBlank()) {
            nombreFinal = validarNombre(nombre);
            exigirNombreLibre(nombreFinal, id);
        }
        String descripcionFinal = descripcion != null ? limpiar(descripcion) : categoria.getDescripcion();
        categoria.actualizar(nombreFinal, descripcionFinal);
        return categoriaRepositoryPort.guardar(categoria);
    }

    @Override
    public void eliminar(Long id) {
        buscarPorId(id);
        if (herramientaRepositoryPort.existePorCategoria(id)) {
            throw new ConflictoException("La categoría tiene herramientas asociadas y no se puede eliminar. "
                    + "Mueva esas herramientas a otra categoría primero.");
        }
        categoriaRepositoryPort.eliminar(id);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("El nombre de la categoría es obligatorio.");
        }
        return nombre.trim();
    }

    private void exigirNombreLibre(String nombre, Long idActual) {
        categoriaRepositoryPort.buscarPorNombre(nombre)
                .filter(existente -> !existente.getId().equals(idActual))
                .ifPresent(existente -> {
                    throw new ConflictoException("Ya existe una categoría con el nombre '" + nombre + "'.");
                });
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
