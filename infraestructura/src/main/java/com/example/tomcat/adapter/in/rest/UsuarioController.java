package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.adapter.in.rest.dto.UsuarioResponse;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final GestionarUsuarioUseCase gestionarUsuarioUseCase;
    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;

    public UsuarioController(GestionarUsuarioUseCase gestionarUsuarioUseCase, RegistrarUsuarioUseCase registrarUsuarioUseCase) {
        this.gestionarUsuarioUseCase = gestionarUsuarioUseCase;
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
    }

    /** Solo ADMIN (ver SecurityConfig): da de alta proveedores o clientes. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(registrarUsuarioUseCase.registrar(
                request.nombre(),
                request.email(),
                request.password(),
                request.rol()
        )));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(@RequestParam(required = false) String rol) {
        return ResponseEntity.ok(resolverListado(rol).stream().map(UsuarioResponse::from).toList());
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<UsuarioResponse>> listarClientes() {
        return ResponseEntity.ok(gestionarUsuarioUseCase.listarClientes().stream().map(UsuarioResponse::from).toList());
    }

    @GetMapping("/proveedores")
    public ResponseEntity<List<UsuarioResponse>> listarProveedores() {
        return ResponseEntity.ok(gestionarUsuarioUseCase.listarProveedores().stream().map(UsuarioResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(UsuarioResponse.from(gestionarUsuarioUseCase.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id, @RequestBody ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(UsuarioResponse.from(gestionarUsuarioUseCase.actualizar(
                id,
                request.nombre(),
                request.email(),
                request.activo(),
                request.password()
        )));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gestionarUsuarioUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private List<Usuario> resolverListado(String rol) {
        if (rol == null || rol.isBlank()) {
            List<Usuario> usuarios = new ArrayList<>();
            usuarios.addAll(gestionarUsuarioUseCase.listarClientes());
            usuarios.addAll(gestionarUsuarioUseCase.listarProveedores());
            return usuarios;
        }
        if ("CLIENTE".equalsIgnoreCase(rol)) {
            return gestionarUsuarioUseCase.listarClientes();
        }
        if ("PROVEEDOR".equalsIgnoreCase(rol)) {
            return gestionarUsuarioUseCase.listarProveedores();
        }
        throw new DomainException("Solo se pueden listar clientes y proveedores.");
    }

    public record CrearUsuarioRequest(String nombre, String email, String password, String rol) {}

    public record ActualizarUsuarioRequest(String nombre, String email, Boolean activo, String password) {}
}
