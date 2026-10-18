package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.adapter.in.rest.dto.UsuarioResponse;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/** Solo ADMIN (ver SecurityConfig). */
@Tag(name = "Usuarios")
@ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
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
    @Operation(summary = "Registrar un proveedor o un cliente", description = "**Rol:** ADMIN. Es la única forma de crear proveedores.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Rol no válido. Use ADMIN, PROVEEDOR o CLIENTE.")
    @ApiResponse(responseCode = "409", description = "El correo ya se encuentra registrado.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(registrarUsuarioUseCase.registrar(
                request.nombre(),
                request.email(),
                request.password(),
                request.rol()
        )));
    }

    @Operation(summary = "Listar proveedores y clientes", description = "**Rol:** ADMIN. Sin filtro devuelve primero los clientes y luego los proveedores.")
    @ApiResponse(responseCode = "200", description = "Listado de usuarios")
    @ApiResponse(responseCode = "400", description = "Solo se pueden listar clientes y proveedores.")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(
            @Parameter(description = "Filtra por rol: CLIENTE o PROVEEDOR", example = "PROVEEDOR")
            @RequestParam(required = false) String rol) {
        return ResponseEntity.ok(resolverListado(rol).stream().map(UsuarioResponse::from).toList());
    }

    @Operation(summary = "Listar clientes", description = "**Rol:** ADMIN.")
    @ApiResponse(responseCode = "200", description = "Clientes registrados")
    @GetMapping("/clientes")
    public ResponseEntity<List<UsuarioResponse>> listarClientes() {
        return ResponseEntity.ok(gestionarUsuarioUseCase.listarClientes().stream().map(UsuarioResponse::from).toList());
    }

    @Operation(summary = "Listar proveedores", description = "**Rol:** ADMIN.")
    @ApiResponse(responseCode = "200", description = "Proveedores registrados")
    @GetMapping("/proveedores")
    public ResponseEntity<List<UsuarioResponse>> listarProveedores() {
        return ResponseEntity.ok(gestionarUsuarioUseCase.listarProveedores().stream().map(UsuarioResponse::from).toList());
    }

    @Operation(summary = "Consultar un usuario", description = "**Rol:** ADMIN.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@Parameter(example = "12") @PathVariable Long id) {
        return ResponseEntity.ok(UsuarioResponse.from(gestionarUsuarioUseCase.buscarPorId(id)));
    }

    @Operation(summary = "Editar o desactivar un usuario",
            description = "**Rol:** ADMIN. Edición parcial: solo cambia lo que se envía. Con `activo=false` el usuario "
                    + "ya no puede iniciar sesión y su token deja de funcionar de inmediato.")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "La contraseña debe tener al menos 8 caracteres.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe.")
    @ApiResponse(responseCode = "409", description = "El correo ya se encuentra registrado.")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@Parameter(example = "12") @PathVariable Long id,
                                                      @Valid @RequestBody ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(UsuarioResponse.from(gestionarUsuarioUseCase.actualizar(
                id,
                request.nombre(),
                request.email(),
                request.activo(),
                request.password()
        )));
    }

    @Operation(summary = "Eliminar un usuario",
            description = "**Rol:** ADMIN. Para no perder historial, un cliente con reservas o un proveedor con "
                    + "herramientas no se elimina: se desactiva con `activo=false`.")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe.")
    @ApiResponse(responseCode = "409", description = "El cliente tiene reservas registradas y no se puede eliminar. Desactívelo con activo=false.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@Parameter(example = "41") @PathVariable Long id) {
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
            @Schema(example = "Hernán Ferretería")
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            @Schema(example = "hernan.ferreteria@gmail.com")
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "El formato del correo electrónico no es válido.")
            @Size(max = 150, message = "El correo admite máximo 150 caracteres.")
            String email,
            @Schema(example = "Rentas2026*", description = "Entre 8 y 72 caracteres")
            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
            String password,
            @Schema(example = "PROVEEDOR", allowableValues = {"PROVEEDOR", "CLIENTE"})
            @NotBlank(message = "El rol es obligatorio (PROVEEDOR o CLIENTE).")
            String rol
    ) {}

    /** Todos los campos son opcionales: solo se cambia lo que se envía. */
    public record ActualizarUsuarioRequest(
            @Schema(example = "Juan Carlos Cliente")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            @Schema(example = "juan.cliente@gmail.com")
            @Email(message = "El formato del correo electrónico no es válido.")
            @Size(max = 150, message = "El correo admite máximo 150 caracteres.")
            String email,
            @Schema(example = "true", description = "false desactiva la cuenta")
            Boolean activo,
            @Schema(example = "NuevaClave2026*", description = "Opcional: nueva contraseña")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
            String password
    ) {}
}
