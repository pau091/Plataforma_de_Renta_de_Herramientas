package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/herramientas")
public class HerramientaController {

    private final GestionarHerramientaUseCase gestionarHerramientaUseCase;

    public HerramientaController(GestionarHerramientaUseCase gestionarHerramientaUseCase) {
        this.gestionarHerramientaUseCase = gestionarHerramientaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<Herramienta>> listar(
            @RequestParam(required = false) Boolean disponibles,
            @RequestParam(required = false) Long proveedorId
    ) {
        if (proveedorId != null) {
            return ResponseEntity.ok(gestionarHerramientaUseCase.listarPorProveedor(proveedorId));
        }
        if (Boolean.TRUE.equals(disponibles)) {
            return ResponseEntity.ok(gestionarHerramientaUseCase.listarDisponibles());
        }
        return ResponseEntity.ok(gestionarHerramientaUseCase.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Herramienta> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Herramienta> crear(@RequestBody HerramientaRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.crear(
                request.proveedorId(),
                request.categoriaId(),
                request.nombre(),
                request.descripcion(),
                request.tarifaDiaria(),
                request.imagenUrl()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Herramienta> actualizar(@PathVariable Long id, @RequestBody HerramientaRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.actualizar(
                id,
                request.categoriaId(),
                request.nombre(),
                request.descripcion(),
                request.tarifaDiaria(),
                request.estado(),
                request.imagenUrl()
        ));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Herramienta> cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.cambiarEstado(id, request.estado()));
    }

    @PatchMapping("/{id}/imagen")
    public ResponseEntity<Herramienta> actualizarImagen(@PathVariable Long id, @RequestBody ImagenRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.actualizarImagen(id, request.imagenUrl()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gestionarHerramientaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    public record HerramientaRequest(
            Long proveedorId,
            Long categoriaId,
            String nombre,
            String descripcion,
            BigDecimal tarifaDiaria,
            String estado,
            String imagenUrl
    ) {}

    public record EstadoRequest(String estado) {}

    public record ImagenRequest(String imagenUrl) {}
}
