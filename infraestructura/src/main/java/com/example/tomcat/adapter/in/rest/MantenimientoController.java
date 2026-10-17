package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.port.in.GestionarMantenimientoUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private final GestionarMantenimientoUseCase gestionarMantenimientoUseCase;

    public MantenimientoController(GestionarMantenimientoUseCase gestionarMantenimientoUseCase) {
        this.gestionarMantenimientoUseCase = gestionarMantenimientoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<Mantenimiento>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long herramientaId,
            @RequestParam(required = false) Long reservaId
    ) {
        if (herramientaId != null) {
            return ResponseEntity.ok(gestionarMantenimientoUseCase.listarPorHerramienta(usuario.solicitante(), herramientaId));
        }
        if (reservaId != null) {
            return ResponseEntity.ok(gestionarMantenimientoUseCase.listarPorReserva(usuario.solicitante(), reservaId));
        }
        return ResponseEntity.ok(gestionarMantenimientoUseCase.listar(usuario.solicitante()));
    }

    @PostMapping
    public ResponseEntity<Mantenimiento> registrar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   @RequestBody MantenimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gestionarMantenimientoUseCase.registrar(
                usuario.solicitante(),
                request.herramientaId(),
                request.descripcionProblema(),
                request.costoReparacion()
        ));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<Mantenimiento> cerrar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @PathVariable Long id,
                                                @RequestBody CerrarRequest request) {
        return ResponseEntity.ok(gestionarMantenimientoUseCase.cerrar(usuario.solicitante(), id, request.costoReparacion()));
    }

    public record MantenimientoRequest(Long herramientaId, String descripcionProblema, BigDecimal costoReparacion) {}

    public record CerrarRequest(BigDecimal costoReparacion) {}
}
