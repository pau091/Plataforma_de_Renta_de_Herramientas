package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.ConsultarDisponibilidadUseCase;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.port.in.VencerReservasUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import jakarta.validation.Valid;
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
    private final VencerReservasUseCase vencerReservasUseCase;

    public ReservaController(
            CrearReservaUseCase crearReservaUseCase,
            GestionarReservaUseCase gestionarReservaUseCase,
            ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase,
            VencerReservasUseCase vencerReservasUseCase
    ) {
        this.crearReservaUseCase = crearReservaUseCase;
        this.gestionarReservaUseCase = gestionarReservaUseCase;
        this.consultarDisponibilidadUseCase = consultarDisponibilidadUseCase;
        this.vencerReservasUseCase = vencerReservasUseCase;
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
            @RequestParam(required = false) Long herramientaId,
            @RequestParam(required = false) String estado
    ) {
        return ResponseEntity.ok(gestionarReservaUseCase.listar(
                usuario.solicitante(), clienteId, proveedorId, herramientaId, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.buscarPorId(usuario.solicitante(), id));
    }

    /** Cambia las fechas de una reserva PENDIENTE (cliente que la hizo o ADMIN). */
    @PutMapping("/{id}")
    public ResponseEntity<Reserva> modificar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @PathVariable Long id,
                                             @Valid @RequestBody ModificarReservaRequest request) {
        return ResponseEntity.ok(gestionarReservaUseCase.modificarFechas(
                usuario.solicitante(), id, request.fechaInicio(), request.fechaFin()));
    }

    /**
     * Cancela una reserva PENDIENTE o ACEPTADA sin pagar (cliente que la hizo o ADMIN).
     * No se borra: queda CANCELADA en el historial y libera las fechas.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Reserva> cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.cancelar(usuario.solicitante(), id));
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

    /**
     * Solo ADMIN: ejecuta ya el vencimiento que normalmente corre cada noche.
     * Devuelve las reservas que pasaron a VENCIDA.
     */
    @PostMapping("/vencer")
    public ResponseEntity<List<Reserva>> vencer() {
        return ResponseEntity.ok(vencerReservasUseCase.vencerReservas());
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
            // Que no sea pasada lo valida CrearReservaService con el reloj de app.zona-horaria.
            @NotNull(message = "La fecha de inicio es obligatoria.")
            LocalDate fechaInicio,
            @NotNull(message = "La fecha fin es obligatoria.")
            LocalDate fechaFin
    ) {}

    public record ModificarReservaRequest(
            @NotNull(message = "La fecha de inicio es obligatoria.")
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
