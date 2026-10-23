package com.example.tomcat.repository;

import com.example.tomcat.model.EntradaListaEspera;

import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepositoryPort {
    EntradaListaEspera guardar(EntradaListaEspera entrada);
    Optional<EntradaListaEspera> buscarPorId(Long id);
    /** Más recientes primero. */
    List<EntradaListaEspera> listarPorCliente(Long clienteId);
    /** Entradas ESPERANDO o NOTIFICADA de la herramienta, por orden de llegada (la primera es la primera de la fila). */
    List<EntradaListaEspera> listarActivasPorHerramienta(Long herramientaId);
}
