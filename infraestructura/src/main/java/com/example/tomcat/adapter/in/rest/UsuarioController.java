package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.adapter.in.rest.dto.UsuarioResponse;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
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
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
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

    public record CrearUsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "El formato del correo electrónico no es válido.")
            @Size(max = 150, message = "El correo admite máximo 150 caracteres.")
            String email,
            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
            String password,
            @NotBlank(message = "El rol es obligatorio (PROVEEDOR o CLIENTE).")
            String rol
    ) {}

    /** Todos los campos son opcionales: solo se cambia lo que se envía. */
    public record ActualizarUsuarioRequest(
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            @Email(message = "El formato del correo electrónico no es válido.")
            @Size(max = 150, message = "El correo admite máximo 150 caracteres.")
            String email,
            Boolean activo,
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
            String password
    ) {}
}
