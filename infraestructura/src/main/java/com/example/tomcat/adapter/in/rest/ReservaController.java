package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.ConsultarDisponibilidadUseCase;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /** La reserva queda PENDIENTE hasta que el proveedor la acepte o la rechace. */
    @PostMapping
    public ResponseEntity<Reserva> crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @Valid @RequestBody ReservaRequest request) {
        // clienteId solo se usa cuando quien reserva es ADMIN; un CLIENTE siempre reserva a su nombre.
        return ResponseEntity.status(HttpStatus.CREATED).body(crearReservaUseCase.crear(
                usuario.solicitante(),
                request.clienteId(),
                request.herramientaId(),
                request.fechaInicio(),
                request.fechaFin()
        ));
    }

    @GetMapping
    public ResponseEntity<List<Reserva>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long herramientaId
    ) {
        return ResponseEntity.ok(gestionarReservaUseCase.listar(usuario.solicitante(), clienteId, proveedorId, herramientaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.buscarPorId(usuario.solicitante(), id));
    }

    @PostMapping("/{id}/aceptar")
    public ResponseEntity<Reserva> aceptar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.aceptar(usuario.solicitante(), id));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Reserva> rechazar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.rechazar(usuario.solicitante(), id));
    }

    @PostMapping("/{id}/entregar")
    public ResponseEntity<Reserva> entregar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.entregar(usuario.solicitante(), id));
    }

    @PostMapping("/{id}/devolver")
    public ResponseEntity<Reserva> devolver(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @PathVariable Long id,
                                            @Valid @RequestBody DevolucionRequest request) {
        return ResponseEntity.ok(gestionarReservaUseCase.devolver(
                usuario.solicitante(), id, request.estadoEquipo(), request.observaciones()));
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

    /** clienteId es opcional: solo lo usa un ADMIN que reserva en nombre de un cliente. */
    public record ReservaRequest(
            Long clienteId,
            @NotNull(message = "La herramienta es obligatoria.")
            Long herramientaId,
            @NotNull(message = "La fecha de inicio es obligatoria.")
            @FutureOrPresent(message = "La fecha de inicio no puede ser pasada.")
            LocalDate fechaInicio,
            @NotNull(message = "La fecha fin es obligatoria.")
            LocalDate fechaFin
    ) {}

    public record DevolucionRequest(
            @NotBlank(message = "El estado del equipo es obligatorio (BUENO, DANADO o PERDIDO).")
            String estadoEquipo,
            @Size(max = 1000, message = "Las observaciones admiten máximo 1000 caracteres.")
            String observaciones
    ) {}

    public record DisponibilidadResponse(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin, boolean disponible) {}
}
