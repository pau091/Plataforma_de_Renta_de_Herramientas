package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.AutenticarUsuarioUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import com.example.tomcat.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            RegistrarUsuarioUseCase registrarUsuarioUseCase,
            AutenticarUsuarioUseCase autenticarUsuarioUseCase,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.autenticarUsuarioUseCase = autenticarUsuarioUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** Registro público: siempre crea una cuenta CLIENTE. El campo rol es opcional y solo admite CLIENTE. */
    @Operation(summary = "Registrar un cliente",
            description = "Registro público. Siempre crea una cuenta **CLIENTE** y devuelve su token, listo para usar. "
                    + "Los proveedores los registra el administrador con `POST /api/usuarios`.")
    @ApiResponse(responseCode = "201", description = "Cliente registrado; incluye el token JWT")
    @ApiResponse(responseCode = "400", description = "La petición tiene datos inválidos.")
    @ApiResponse(responseCode = "403", description = "El registro público solo permite cuentas de CLIENTE. Los proveedores los registra el administrador.")
    @ApiResponse(responseCode = "409", description = "El correo ya se encuentra registrado.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = registrarUsuarioUseCase.registrarCliente(
                request.nombre(),
                request.email(),
                request.password(),
                request.rol()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(usuario));
    }

    @Operation(summary = "Iniciar sesión",
            description = "Valida correo y contraseña y devuelve un token JWT válido por 24 horas. "
                    + "Úselo en **Authorize** o en el encabezado `Authorization: Bearer <token>`.")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas")
    @ApiResponse(responseCode = "400", description = "La petición tiene datos inválidos.")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas.")
    @ApiResponse(responseCode = "403", description = "El usuario está inactivo. Contacte al administrador de la plataforma.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = autenticarUsuarioUseCase.autenticar(request.email(), request.password());
        return ResponseEntity.ok(toAuthResponse(usuario));
    }

    private AuthResponse toAuthResponse(Usuario usuario) {
        String token = jwtTokenProvider.generarToken(usuario.getEmail(), usuario.getRol(), usuario.getId());
        return new AuthResponse(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol().name()
        );
    }

    public record RegistroRequest(
            @Schema(example = "Laura Gómez")
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
            String nombre,
            @Schema(example = "laura.gomez@gmail.com")
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "El formato del correo electrónico no es válido.")
            @Size(max = 150, message = "El correo admite máximo 150 caracteres.")
            String email,
            @Schema(example = "Rentas2026*", description = "Entre 8 y 72 caracteres")
            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
            String password,
            @Schema(example = "CLIENTE", description = "Opcional. Solo se admite CLIENTE")
            String rol
    ) {}

    public record LoginRequest(
            @Schema(example = "juan.cliente@gmail.com")
            @NotBlank(message = "El correo es obligatorio.") String email,
            @Schema(example = "Rentas2026*")
            @NotBlank(message = "La contraseña es obligatoria.") String password
    ) {}

    @Schema(description = "Sesión iniciada: token JWT y datos básicos del usuario")
    public record AuthResponse(
            @Schema(example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqdWFuLmNsaWVudGVAZ21haWwuY29tIiwicm9sIjoiQ0xJRU5URSIsInVzdWFyaW9JZCI6MTJ9.firma",
                    description = "Token JWT (HS512, 24 h). Enviar como 'Authorization: Bearer <token>'")
            String token,
            @Schema(example = "12") Long usuarioId,
            @Schema(example = "Juan Cliente") String nombre,
            @Schema(example = "juan.cliente@gmail.com") String email,
            @Schema(example = "CLIENTE", allowableValues = {"ADMIN", "PROVEEDOR", "CLIENTE"}) String rol
    ) {}
}
