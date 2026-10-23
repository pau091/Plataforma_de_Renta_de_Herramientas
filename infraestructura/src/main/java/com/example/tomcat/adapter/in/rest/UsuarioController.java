package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.adapter.in.rest.dto.UsuarioResponse;
import com.example.tomcat.exception.DomainException;
import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.GestionarUsuarioUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import com.example.tomcat.port.in.SancionarUsuarioUseCase;
import com.example.tomcat.port.in.dto.Sancion;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final SancionarUsuarioUseCase sancionarUsuarioUseCase;

    public UsuarioController(GestionarUsuarioUseCase gestionarUsuarioUseCase, RegistrarUsuarioUseCase registrarUsuarioUseCase,
                             SancionarUsuarioUseCase sancionarUsuarioUseCase) {
        this.gestionarUsuarioUseCase = gestionarUsuarioUseCase;
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.sancionarUsuarioUseCase = sancionarUsuarioUseCase;
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

    @Operation(summary = "Listar proveedores y clientes", description = "**Rol:** ADMIN. Sin filtro devuelve primero los "
            + "clientes y luego los proveedores. Con `rol=ADMIN` lista los administradores (útil para cambiarles el rol).")
    @ApiResponse(responseCode = "200", description = "Listado de usuarios")
    @ApiResponse(responseCode = "400", description = "Rol no válido. Use CLIENTE, PROVEEDOR o ADMIN.")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(
            @Parameter(description = "Filtra por rol: CLIENTE, PROVEEDOR o ADMIN", example = "PROVEEDOR")
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

    @Operation(summary = "Cambiar el rol de un usuario (ascender o bajar de rango)",
            description = "**Rol:** solo el **administrador principal** (`app.admin-principal`, por defecto admin@rentas.com). "
                    + "Pasa a un usuario ya registrado a CLIENTE, PROVEEDOR o ADMIN. No se puede cambiar el rol del "
                    + "administrador principal, ni pasar a CLIENTE a un proveedor con herramientas. La API aplica el rol "
                    + "de inmediato; el usuario debe volver a iniciar sesión para ver el menú de su nuevo rol.")
    @ApiResponse(responseCode = "200", description = "Rol actualizado")
    @ApiResponse(responseCode = "400", description = "Rol no válido. Use ADMIN, PROVEEDOR o CLIENTE.")
    @ApiResponse(responseCode = "403", description = "Solo el administrador principal puede cambiar roles.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe.")
    @ApiResponse(responseCode = "409", description = "El rol del administrador principal no se puede cambiar.")
    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                      @Parameter(example = "12") @PathVariable Long id,
                                                      @Valid @RequestBody CambiarRolRequest request) {
        return ResponseEntity.ok(UsuarioResponse.from(
                gestionarUsuarioUseCase.cambiarRol(usuario.solicitante(), id, request.rol())));
    }

    @Operation(summary = "Suspender a un proveedor o cliente",
            description = "**Rol:** ADMIN. Bloquea la cuenta durante `dias` días (1 a 365) y la **expulsa de inmediato**: "
                    + "todos sus tokens JWT vigentes pasan a la lista negra, así que su siguiente petición recibe 401 con el "
                    + "motivo, sin esperar a que el token expire. Al terminar el plazo puede volver a iniciar sesión.")
    @ApiResponse(responseCode = "200", description = "Usuario suspendido; incluye cuántas sesiones se cerraron")
    @ApiResponse(responseCode = "400", description = "Debe indicar el motivo de la sanción.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe.")
    @PostMapping("/{id}/suspension")
    public ResponseEntity<SancionResponse> suspender(@Parameter(example = "12") @PathVariable Long id,
                                                     @Valid @RequestBody SuspensionRequest request) {
        return ResponseEntity.ok(SancionResponse.from(sancionarUsuarioUseCase.suspender(id, request.dias(), request.motivo())));
    }

    @Operation(summary = "Banear a un proveedor o cliente",
            description = "**Rol:** ADMIN. Bloqueo **indefinido**, hasta que un administrador lo reactive. Igual que la "
                    + "suspensión, revoca al instante todos sus tokens vigentes (lista negra).")
    @ApiResponse(responseCode = "200", description = "Usuario baneado; incluye cuántas sesiones se cerraron")
    @ApiResponse(responseCode = "400", description = "El usuario ya está baneado.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe.")
    @PostMapping("/{id}/baneo")
    public ResponseEntity<SancionResponse> banear(@Parameter(example = "12") @PathVariable Long id,
                                                  @Valid @RequestBody BaneoRequest request) {
        return ResponseEntity.ok(SancionResponse.from(sancionarUsuarioUseCase.banear(id, request.motivo())));
    }

    @Operation(summary = "Levantar la suspensión o el baneo",
            description = "**Rol:** ADMIN. El usuario puede volver a iniciar sesión. Sus tokens anteriores siguen revocados: "
                    + "debe obtener uno nuevo con el login.")
    @ApiResponse(responseCode = "200", description = "Usuario reactivado")
    @ApiResponse(responseCode = "400", description = "El usuario no tiene una suspensión ni un baneo vigente.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe.")
    @PostMapping("/{id}/reactivacion")
    public ResponseEntity<UsuarioResponse> reactivar(@Parameter(example = "12") @PathVariable Long id) {
        return ResponseEntity.ok(UsuarioResponse.from(sancionarUsuarioUseCase.reactivar(id)));
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
        if ("ADMIN".equalsIgnoreCase(rol)) {
            return gestionarUsuarioUseCase.listarAdministradores();
        }
        throw new DomainException("Rol no válido. Use CLIENTE, PROVEEDOR o ADMIN.");
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

    public record SuspensionRequest(
            @Schema(example = "7", description = "Duración de la suspensión en días (1 a 365)")
            @NotNull(message = "Indique cuántos días dura la suspensión.")
            @Min(value = 1, message = "La suspensión debe durar al menos 1 día.")
            @Max(value = 365, message = "La suspensión admite máximo 365 días.")
            Integer dias,
            @Schema(example = "Cancelaciones reiteradas sin aviso")
            @NotBlank(message = "Debe indicar el motivo de la sanción.")
            @Size(max = 255, message = "El motivo admite máximo 255 caracteres.")
            String motivo
    ) {}

    public record BaneoRequest(
            @Schema(example = "Fraude en pagos")
            @NotBlank(message = "Debe indicar el motivo de la sanción.")
            @Size(max = 255, message = "El motivo admite máximo 255 caracteres.")
            String motivo
    ) {}

    public record SancionResponse(
            UsuarioResponse usuario,
            @Schema(example = "2", description = "Sesiones abiertas (tokens vigentes) que se revocaron al instante")
            int tokensRevocados
    ) {
        static SancionResponse from(Sancion sancion) {
            return new SancionResponse(UsuarioResponse.from(sancion.usuario()), sancion.tokensRevocados());
        }
    }

    public record CambiarRolRequest(
            @Schema(example = "PROVEEDOR", allowableValues = {"CLIENTE", "PROVEEDOR", "ADMIN"})
            @NotBlank(message = "El rol es obligatorio (CLIENTE, PROVEEDOR o ADMIN).")
            String rol
    ) {}
}
