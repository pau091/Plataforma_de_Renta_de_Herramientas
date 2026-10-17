package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.AutenticarUsuarioUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import com.example.tomcat.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            String rol
    ) {}

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio.") String email,
            @NotBlank(message = "La contraseña es obligatoria.") String password
    ) {}

    public record AuthResponse(String token, Long usuarioId, String nombre, String email, String rol) {}
}
