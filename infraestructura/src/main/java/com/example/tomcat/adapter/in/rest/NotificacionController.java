package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Notificacion;
import com.example.tomcat.port.in.NotificacionesUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notificaciones", description = "Avisos del usuario autenticado (la campana de la página).")
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionesUseCase notificacionesUseCase;

    public NotificacionController(NotificacionesUseCase notificacionesUseCase) {
        this.notificacionesUseCase = notificacionesUseCase;
    }

    @Operation(summary = "Mis notificaciones", description = "**Roles:** cualquier usuario autenticado. Más recientes primero.")
    @ApiResponse(responseCode = "200", description = "Notificaciones del usuario")
    @GetMapping
    public ResponseEntity<List<Notificacion>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(notificacionesUseCase.listar(usuario.solicitante()));
    }

    @Operation(summary = "Cuántas notificaciones no he leído", description = "**Roles:** cualquier usuario autenticado. "
            + "Es el número que muestra la campana.")
    @ApiResponse(responseCode = "200", description = "{\"total\": 2}")
    @GetMapping("/no-leidas")
    public ResponseEntity<Map<String, Long>> noLeidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(Map.of("total", notificacionesUseCase.contarNoLeidas(usuario.solicitante())));
    }

    @Operation(summary = "Marcar una notificación como leída", description = "**Roles:** el dueño de la notificación.")
    @ApiResponse(responseCode = "200", description = "Notificación marcada como leída")
    @ApiResponse(responseCode = "403", description = "La notificación es de otro usuario.")
    @ApiResponse(responseCode = "404", description = "La notificación no existe.")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Notificacion> marcarLeida(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                    @Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(notificacionesUseCase.marcarLeida(usuario.solicitante(), id));
    }

    @Operation(summary = "Marcar todas como leídas", description = "**Roles:** cualquier usuario autenticado.")
    @ApiResponse(responseCode = "204", description = "Todas marcadas como leídas")
    @PostMapping("/leidas")
    public ResponseEntity<Void> marcarTodas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        notificacionesUseCase.marcarTodasLeidas(usuario.solicitante());
        return ResponseEntity.noContent().build();
    }
}
