package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.port.in.GestionarHerramientaUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/herramientas")
public class HerramientaController {

    private final GestionarHerramientaUseCase gestionarHerramientaUseCase;

    public HerramientaController(GestionarHerramientaUseCase gestionarHerramientaUseCase) {
        this.gestionarHerramientaUseCase = gestionarHerramientaUseCase;
    }

    /** Catálogo con filtros combinables: categoría, proveedor, solo disponibles y texto (q) en nombre o descripción. */
    @GetMapping
    public ResponseEntity<List<Herramienta>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean disponibles,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.buscar(
                categoriaId, proveedorId, Boolean.TRUE.equals(disponibles), q));
    }

    /** Inventario del proveedor autenticado. */
    @GetMapping("/mias")
    public ResponseEntity<List<Herramienta>> listarMias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.listarPorProveedor(usuario.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Herramienta> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Herramienta> crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Valid @RequestBody CrearHerramientaRequest request) {
        // proveedorId solo se usa cuando quien registra es ADMIN; un PROVEEDOR siempre registra a su nombre.
        return ResponseEntity.status(HttpStatus.CREATED).body(gestionarHerramientaUseCase.crear(
                usuario.solicitante(),
                request.proveedorId(),
                request.categoriaId(),
                request.nombre(),
                request.descripcion(),
                request.tarifaDiaria(),
                request.imagenUrl()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Herramienta> actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody ActualizarHerramientaRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.actualizar(
                usuario.solicitante(),
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
    public ResponseEntity<Herramienta> cambiarEstado(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.cambiarEstado(usuario.solicitante(), id, request.estado()));
    }

    @PatchMapping("/{id}/imagen")
    public ResponseEntity<Herramienta> actualizarImagen(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody ImagenRequest request) {
        return ResponseEntity.ok(gestionarHerramientaUseCase.actualizarImagen(usuario.solicitante(), id, request.imagenUrl()));
    }

    /** Sube el archivo de imagen de la herramienta (campo "archivo"; JPG, PNG, WEBP o GIF, máx. 5 MB). */
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Herramienta> subirImagen(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   @PathVariable Long id,
                                                   @RequestParam("archivo") MultipartFile archivo) throws IOException {
        return ResponseEntity.ok(gestionarHerramientaUseCase.subirImagen(usuario.solicitante(), id, archivo.getBytes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        gestionarHerramientaUseCase.eliminar(usuario.solicitante(), id);
        return ResponseEntity.noContent().build();
    }

    /** proveedorId solo lo usa un ADMIN; un PROVEEDOR siempre registra a su nombre. */
    public record CrearHerramientaRequest(
            Long proveedorId,
            @NotNull(message = "La categoría es obligatoria.")
            Long categoriaId,
            @NotBlank(message = "El nombre de la herramienta es obligatorio.")
            @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
            String nombre,
            String descripcion,
            @NotNull(message = "La tarifa diaria es obligatoria.")
            @Positive(message = "La tarifa diaria debe ser mayor que cero.")
            @Digits(integer = 10, fraction = 2, message = "La tarifa admite hasta 10 enteros y 2 decimales.")
            BigDecimal tarifaDiaria,
            @Size(max = 500, message = "La URL de la imagen admite máximo 500 caracteres.")
            String imagenUrl
    ) {}

    /** Todos los campos son opcionales: solo se cambia lo que se envía. */
    public record ActualizarHerramientaRequest(
            Long categoriaId,
            @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
            String nombre,
            String descripcion,
            @Positive(message = "La tarifa diaria debe ser mayor que cero.")
            @Digits(integer = 10, fraction = 2, message = "La tarifa admite hasta 10 enteros y 2 decimales.")
            BigDecimal tarifaDiaria,
            String estado,
            @Size(max = 500, message = "La URL de la imagen admite máximo 500 caracteres.")
            String imagenUrl
    ) {}

    public record EstadoRequest(
            @NotBlank(message = "El estado es obligatorio (DISPONIBLE, MANTENIMIENTO o FUERA_DE_SERVICIO).")
            String estado
    ) {}

    public record ImagenRequest(
            @NotBlank(message = "La URL de la imagen es obligatoria.")
            @Size(max = 500, message = "La URL de la imagen admite máximo 500 caracteres.")
            String imagenUrl
    ) {}
}
