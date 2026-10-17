package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.ConsultarDisponibilidadUseCase;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final CrearReservaUseCase crearReservaUseCase;
    private final GestionarReservaUseCase gestionarReservaUseCase;
    private final ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;

    public ReservaController(
            CrearReservaUseCase crearReservaUseCase,
            GestionarReservaUseCase gestionarReservaUseCase,
            ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase
    ) {
        this.crearReservaUseCase = crearReservaUseCase;
        this.gestionarReservaUseCase = gestionarReservaUseCase;
        this.consultarDisponibilidadUseCase = consultarDisponibilidadUseCase;
    }

    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody ReservaRequest request) {
        return ResponseEntity.ok(crearReservaUseCase.crear(
                request.clienteId(),
                request.herramientaId(),
                request.fechaInicio(),
                request.fechaFin()
        ));
    }

    @GetMapping
    public ResponseEntity<List<Reserva>> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long herramientaId
    ) {
        if (clienteId != null) {
            return ResponseEntity.ok(gestionarReservaUseCase.listarPorCliente(clienteId));
        }
        if (proveedorId != null) {
            return ResponseEntity.ok(gestionarReservaUseCase.listarPorProveedor(proveedorId));
        }
        if (herramientaId != null) {
            return ResponseEntity.ok(gestionarReservaUseCase.listarPorHerramienta(herramientaId));
        }
        return ResponseEntity.ok(gestionarReservaUseCase.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.buscarPorId(id));
    }

    @PostMapping("/{id}/aceptar")
    public ResponseEntity<Reserva> aceptar(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.aceptar(id));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Reserva> rechazar(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.rechazar(id));
    }

    @PostMapping("/{id}/entregar")
    public ResponseEntity<Reserva> entregar(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.entregar(id));
    }

    @PostMapping("/{id}/devolver")
    public ResponseEntity<Reserva> devolver(@PathVariable Long id, @RequestBody DevolucionRequest request) {
        return ResponseEntity.ok(gestionarReservaUseCase.devolver(id, request.estadoEquipo(), request.observaciones()));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<Herramienta>> disponibles(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        return ResponseEntity.ok(consultarDisponibilidadUseCase.listarDisponiblesEnRango(fechaInicio, fechaFin));
    }

    @GetMapping("/disponibilidad/{herramientaId}")
    public ResponseEntity<DisponibilidadResponse> disponible(
            @PathVariable Long herramientaId,
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin
    ) {
        boolean disponible = consultarDisponibilidadUseCase.estaDisponible(herramientaId, fechaInicio, fechaFin);
        return ResponseEntity.ok(new DisponibilidadResponse(herramientaId, fechaInicio, fechaFin, disponible));
    }

    public record ReservaRequest(Long clienteId, Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {}

    public record DevolucionRequest(String estadoEquipo, String observaciones) {}

    public record DisponibilidadResponse(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, boolean disponible) {}
}
