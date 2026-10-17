package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Categoria;
import com.example.tomcat.port.in.GestionarCategoriaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Consultar categorías: cualquier usuario autenticado. Crear, editar y eliminar: solo ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final GestionarCategoriaUseCase gestionarCategoriaUseCase;

    public CategoriaController(GestionarCategoriaUseCase gestionarCategoriaUseCase) {
        this.gestionarCategoriaUseCase = gestionarCategoriaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<Categoria>> listar() {
        return ResponseEntity.ok(gestionarCategoriaUseCase.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarCategoriaUseCase.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Categoria> crear(@Valid @RequestBody CrearCategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gestionarCategoriaUseCase.crear(request.nombre(), request.descripcion()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarCategoriaRequest request) {
        return ResponseEntity.ok(gestionarCategoriaUseCase.actualizar(id, request.nombre(), request.descripcion()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gestionarCategoriaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    public record CrearCategoriaRequest(
            @NotBlank(message = "El nombre de la categoría es obligatorio.")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            String descripcion
    ) {}

    /** Ambos campos son opcionales: solo se cambia lo que se envía. */
    public record ActualizarCategoriaRequest(
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            String descripcion
    ) {}
}
