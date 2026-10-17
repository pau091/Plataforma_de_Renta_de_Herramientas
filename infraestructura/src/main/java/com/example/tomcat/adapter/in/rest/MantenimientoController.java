package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.port.in.GestionarMantenimientoUseCase;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(required = false) Long herramientaId,
            @RequestParam(required = false) Long reservaId
    ) {
        if (herramientaId != null) {
            return ResponseEntity.ok(gestionarMantenimientoUseCase.listarPorHerramienta(herramientaId));
        }
        if (reservaId != null) {
            return ResponseEntity.ok(gestionarMantenimientoUseCase.listarPorReserva(reservaId));
        }
        return ResponseEntity.ok(gestionarMantenimientoUseCase.listar());
    }

    @PostMapping
    public ResponseEntity<Mantenimiento> registrar(@RequestBody MantenimientoRequest request) {
        return ResponseEntity.ok(gestionarMantenimientoUseCase.registrar(
                request.herramientaId(),
                request.descripcionProblema(),
                request.costoReparacion()
        ));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<Mantenimiento> cerrar(@PathVariable Long id, @RequestBody CerrarRequest request) {
        return ResponseEntity.ok(gestionarMantenimientoUseCase.cerrar(id, request.costoReparacion()));
    }

    public record MantenimientoRequest(Long herramientaId, String descripcionProblema, BigDecimal costoReparacion) {}

    public record CerrarRequest(BigDecimal costoReparacion) {}
}
