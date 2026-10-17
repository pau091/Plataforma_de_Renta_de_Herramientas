package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Usuario;
import com.example.tomcat.port.in.AutenticarUsuarioUseCase;
import com.example.tomcat.port.in.RegistrarUsuarioUseCase;
import com.example.tomcat.security.JwtTokenProvider;
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

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@RequestBody RegistroRequest request) {
        Usuario usuario = registrarUsuarioUseCase.registrar(
                request.nombre(),
                request.email(),
                request.password(),
                request.rol()
        );
        return ResponseEntity.ok(toAuthResponse(usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
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

    public record RegistroRequest(String nombre, String email, String password, String rol) {}

    public record LoginRequest(String email, String password) {}

    public record AuthResponse(String token, Long usuarioId, String nombre, String email, String rol) {}
}
