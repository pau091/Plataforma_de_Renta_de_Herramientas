package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Categoria;
import com.example.tomcat.port.in.GestionarCategoriaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Consultar categorías: cualquier usuario autenticado. Crear, editar y eliminar: solo ADMIN (ver SecurityConfig). */
@Tag(name = "Categorías")
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final GestionarCategoriaUseCase gestionarCategoriaUseCase;

    public CategoriaController(GestionarCategoriaUseCase gestionarCategoriaUseCase) {
        this.gestionarCategoriaUseCase = gestionarCategoriaUseCase;
    }

    @Operation(summary = "Listar categorías", description = "**Roles:** cualquier usuario autenticado.")
    @GetMapping
    public ResponseEntity<List<Categoria>> listar() {
        return ResponseEntity.ok(gestionarCategoriaUseCase.listar());
    }

    @Operation(summary = "Consultar una categoría", description = "**Roles:** cualquier usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Categoría encontrada")
    @ApiResponse(responseCode = "404", description = "La categoría no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtener(@Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarCategoriaUseCase.buscarPorId(id));
    }

    @Operation(summary = "Crear una categoría", description = "**Rol:** ADMIN. El nombre es único (sin distinguir mayúsculas).")
    @ApiResponse(responseCode = "201", description = "Categoría creada")
    @ApiResponse(responseCode = "400", description = "La petición tiene datos inválidos.")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con el nombre 'Concreto'.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<Categoria> crear(@Valid @RequestBody CrearCategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gestionarCategoriaUseCase.crear(request.nombre(), request.descripcion()));
    }

    @Operation(summary = "Editar una categoría", description = "**Rol:** ADMIN. Edición parcial: solo cambia lo que se envía.")
    @ApiResponse(responseCode = "200", description = "Categoría actualizada")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @ApiResponse(responseCode = "404", description = "La categoría no existe.")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con el nombre 'Concreto'.")
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizar(@Parameter(example = "1") @PathVariable Long id,
                                                @Valid @RequestBody ActualizarCategoriaRequest request) {
        return ResponseEntity.ok(gestionarCategoriaUseCase.actualizar(id, request.nombre(), request.descripcion()));
    }

    @Operation(summary = "Eliminar una categoría", description = "**Rol:** ADMIN. No se elimina si tiene herramientas asociadas.")
    @ApiResponse(responseCode = "204", description = "Categoría eliminada")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @ApiResponse(responseCode = "404", description = "La categoría no existe.")
    @ApiResponse(responseCode = "409", description = "La categoría tiene herramientas asociadas y no se puede eliminar. Mueva esas herramientas a otra categoría primero.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@Parameter(example = "1") @PathVariable Long id) {
        gestionarCategoriaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    public record CrearCategoriaRequest(
            @Schema(example = "Topografía")
            @NotBlank(message = "El nombre de la categoría es obligatorio.")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            @Schema(example = "Estaciones totales, GPS y equipos de medición de terreno")
            String descripcion
    ) {}

    /** Ambos campos son opcionales: solo se cambia lo que se envía. */
    public record ActualizarCategoriaRequest(
            @Schema(example = "Topografía y Medición")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            @Schema(example = "Estaciones totales, niveles y GPS")
            String descripcion
    ) {}
}
