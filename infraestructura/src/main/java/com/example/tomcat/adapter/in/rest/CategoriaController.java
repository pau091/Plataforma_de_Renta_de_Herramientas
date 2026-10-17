package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Categoria;
import com.example.tomcat.repository.CategoriaRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaRepositoryPort categoriaRepositoryPort;

    public CategoriaController(CategoriaRepositoryPort categoriaRepositoryPort) {
        this.categoriaRepositoryPort = categoriaRepositoryPort;
    }

    @GetMapping
    public ResponseEntity<List<Categoria>> listar() {
        return ResponseEntity.ok(categoriaRepositoryPort.listar());
    }
}
