package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Herramienta;
import com.example.tomcat.model.Reserva;
import com.example.tomcat.port.in.ConsultarDisponibilidadUseCase;
import com.example.tomcat.port.in.CrearReservaUseCase;
import com.example.tomcat.port.in.GestionarReservaUseCase;
import com.example.tomcat.port.in.VencerReservasUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Reservas")
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private static final String SOLO_PROVEEDOR_DUENO = "Solo el proveedor dueño de la herramienta puede gestionar esta reserva.";
    private static final String SOLO_TITULAR = "Solo el cliente que hizo la reserva puede modificarla o cancelarla.";

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
    @Operation(summary = "Reservar una herramienta",
            description = "**Roles:** CLIENTE o PROVEEDOR (la reserva queda a su nombre; `clienteId` se ignora; un proveedor no puede reservar "
                    + "sus propias herramientas) o ADMIN (debe indicar `clienteId`). "
                    + "Antes de crearla se valida que la herramienta esté DISPONIBLE, que la fecha de inicio no sea pasada y que "
                    + "no se cruce con otra reserva activa. Nace **PENDIENTE**: el proveedor tiene 1 hora para aceptarla.")
    @ApiResponse(responseCode = "201", description = "Reserva creada en estado PENDIENTE")
    @ApiResponse(responseCode = "400", description = "No se puede reservar con una fecha de inicio pasada.")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @ApiResponse(responseCode = "409", description = "La herramienta ya tiene una reserva en esas fechas.")
    @ResponseStatus(HttpStatus.CREATED)
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

    @Operation(summary = "Historial de reservas",
            description = "**Roles:** CLIENTE (sus reservas), PROVEEDOR (las de sus herramientas; con `clienteId=<su id>`, las que él "
                    + "alquiló como cliente) o ADMIN (todas). "
                    + "Los filtros solo acotan dentro de lo que el rol puede ver.")
    @ApiResponse(responseCode = "200", description = "Reservas visibles para el usuario")
    @ApiResponse(responseCode = "400", description = "Estado de reserva no válido. Use uno de: ACEPTADA, CANCELADA, CONFIRMADA, ENTREGADA, FINALIZADA, PENDIENTE, RECHAZADA, VENCIDA.")
    @ApiResponse(responseCode = "403", description = "Solo puede consultar sus propias reservas.")
    @GetMapping
    public ResponseEntity<List<Reserva>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Solo reservas de este cliente", example = "12")
            @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Solo reservas de herramientas de este proveedor", example = "2")
            @RequestParam(required = false) Long proveedorId,
            @Parameter(description = "Solo reservas de esta herramienta", example = "1")
            @RequestParam(required = false) Long herramientaId,
            @Parameter(description = "Solo reservas en este estado", example = "PENDIENTE",
                    schema = @Schema(allowableValues = {"PENDIENTE", "ACEPTADA", "CONFIRMADA", "ENTREGADA", "FINALIZADA",
                            "RECHAZADA", "VENCIDA", "CANCELADA"}))
            @RequestParam(required = false) String estado
    ) {
        return ResponseEntity.ok(gestionarReservaUseCase.listar(
                usuario.solicitante(), clienteId, proveedorId, herramientaId, estado));
    }

    @Operation(summary = "Consultar una reserva",
            description = "**Roles:** quien la hizo (cliente o proveedor que alquila), el PROVEEDOR de la herramienta o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Reserva encontrada")
    @ApiResponse(responseCode = "403", description = "No tiene permiso para acceder a esta reserva.")
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @Parameter(example = "7") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.buscarPorId(usuario.solicitante(), id));
    }

    /** Cambia las fechas de una reserva PENDIENTE (cliente que la hizo o ADMIN). */
    @Operation(summary = "Cambiar las fechas de una reserva",
            description = "**Roles:** quien la hizo (cliente o proveedor que alquila) o ADMIN. Solo mientras está **PENDIENTE**; se repiten las "
                    + "validaciones de fechas y de disponibilidad. No reinicia el plazo de 1 hora.")
    @ApiResponse(responseCode = "200", description = "Fechas actualizadas")
    @ApiResponse(responseCode = "400", description = "Solo se pueden modificar las fechas de reservas pendientes.")
    @ApiResponse(responseCode = "403", description = SOLO_TITULAR)
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @ApiResponse(responseCode = "409", description = "La herramienta ya tiene una reserva en esas fechas.")
    @PutMapping("/{id}")
    public ResponseEntity<Reserva> modificar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Parameter(example = "35") @PathVariable Long id,
                                             @Valid @RequestBody ModificarReservaRequest request) {
        return ResponseEntity.ok(gestionarReservaUseCase.modificarFechas(
                usuario.solicitante(), id, request.fechaInicio(), request.fechaFin()));
    }

    /**
     * Cancela una reserva PENDIENTE o ACEPTADA sin pagar (cliente que la hizo o ADMIN).
     * No se borra: queda CANCELADA en el historial y libera las fechas.
     */
    @Operation(summary = "Cancelar una reserva",
            description = "**Roles:** quien la hizo (cliente o proveedor que alquila) o ADMIN. Solo si está PENDIENTE o ACEPTADA sin pagar. "
                    + "No se borra: queda **CANCELADA** en el historial y libera las fechas. "
                    + "Una reserva ya pagada se cancela con el reembolso del pago (`DELETE /api/pagos/{id}`).")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada")
    @ApiResponse(responseCode = "400", description = "La reserva ya está pagada: para cancelarla el administrador debe reembolsar el pago.")
    @ApiResponse(responseCode = "403", description = SOLO_TITULAR)
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Reserva> cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @Parameter(example = "35") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.cancelar(usuario.solicitante(), id));
    }

    @Operation(summary = "Aceptar una solicitud",
            description = "**Roles:** el PROVEEDOR de la herramienta o ADMIN. PENDIENTE → **ACEPTADA**. "
                    + "Desde ese momento el cliente tiene 1 hora para pagar.")
    @ApiResponse(responseCode = "200", description = "Reserva aceptada")
    @ApiResponse(responseCode = "400", description = "Solo se pueden aceptar reservas pendientes.")
    @ApiResponse(responseCode = "403", description = SOLO_PROVEEDOR_DUENO)
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @ApiResponse(responseCode = "409", description = "La reserva venció: el plazo para aceptarla era de 60 minutos desde que se creó.")
    @PostMapping("/{id}/aceptar")
    public ResponseEntity<Reserva> aceptar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @Parameter(example = "35") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.aceptar(usuario.solicitante(), id));
    }

    @Operation(summary = "Rechazar una solicitud", description = "**Roles:** el PROVEEDOR de la herramienta o ADMIN. PENDIENTE → **RECHAZADA**.")
    @ApiResponse(responseCode = "200", description = "Reserva rechazada")
    @ApiResponse(responseCode = "400", description = "Solo se pueden rechazar reservas pendientes.")
    @ApiResponse(responseCode = "403", description = SOLO_PROVEEDOR_DUENO)
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Reserva> rechazar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @Parameter(example = "35") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.rechazar(usuario.solicitante(), id));
    }

    @Operation(summary = "Registrar la entrega",
            description = "**Roles:** el PROVEEDOR de la herramienta o ADMIN. CONFIRMADA (pagada) → **ENTREGADA**; guarda la fecha de entrega.")
    @ApiResponse(responseCode = "200", description = "Entrega registrada")
    @ApiResponse(responseCode = "400", description = "Solo se pueden entregar reservas confirmadas (aceptadas y pagadas).")
    @ApiResponse(responseCode = "403", description = SOLO_PROVEEDOR_DUENO)
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @PostMapping("/{id}/entregar")
    public ResponseEntity<Reserva> entregar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @Parameter(example = "5") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarReservaUseCase.entregar(usuario.solicitante(), id));
    }

    @Operation(summary = "Confirmar la devolución y reportar daños",
            description = "**Roles:** el PROVEEDOR de la herramienta o ADMIN. ENTREGADA → **FINALIZADA**. "
                    + "Con `DANADO` la herramienta pasa a MANTENIMIENTO y se abre un mantenimiento ligado a la reserva; "
                    + "con `PERDIDO` pasa a FUERA_DE_SERVICIO. Aparece en `GET /api/reportes/incidencias`.")
    @ApiResponse(responseCode = "200", description = "Devolución registrada")
    @ApiResponse(responseCode = "400", description = "Estado de devolución no válido. Use BUENO, DANADO o PERDIDO.")
    @ApiResponse(responseCode = "403", description = SOLO_PROVEEDOR_DUENO)
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @PostMapping("/{id}/devolver")
    public ResponseEntity<Reserva> devolver(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            @Parameter(example = "7") @PathVariable Long id,
                                            @Valid @RequestBody DevolucionRequest request) {
        return ResponseEntity.ok(gestionarReservaUseCase.devolver(
                usuario.solicitante(), id, request.estadoEquipo(), request.observaciones()));
    }

    /**
     * Solo ADMIN: ejecuta ya el vencimiento que normalmente corre cada minuto.
     * Devuelve las reservas que pasaron a VENCIDA.
     */
    @Operation(summary = "Vencer reservas ahora",
            description = "**Rol:** ADMIN. Ejecuta de inmediato el vencimiento que la aplicación corre cada minuto: "
                    + "las PENDIENTE sin aceptar y las ACEPTADA sin pagar después de 1 hora (o con la fecha de inicio "
                    + "ya pasada) pasan a **VENCIDA**. Devuelve las reservas vencidas.")
    @ApiResponse(responseCode = "200", description = "Reservas que pasaron a VENCIDA (puede ser una lista vacía)")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @PostMapping("/vencer")
    public ResponseEntity<List<Reserva>> vencer() {
        return ResponseEntity.ok(vencerReservasUseCase.vencerReservas());
    }

    @Operation(summary = "Herramientas disponibles en un rango de fechas",
            description = "**Roles:** cualquier usuario autenticado. Herramientas DISPONIBLES sin reservas activas que se crucen con el rango.")
    @ApiResponse(responseCode = "200", description = "Herramientas libres en esas fechas")
    @ApiResponse(responseCode = "400", description = "La fecha de inicio no puede ser posterior a la fecha fin.")
    @GetMapping("/disponibilidad")
    public ResponseEntity<List<Herramienta>> disponibles(
            @Parameter(description = "Primer día (yyyy-MM-dd)", example = "2026-11-10") @RequestParam LocalDate fechaInicio,
            @Parameter(description = "Último día, incluido (yyyy-MM-dd)", example = "2026-11-12") @RequestParam LocalDate fechaFin
    ) {
        return ResponseEntity.ok(consultarDisponibilidadUseCase.listarDisponiblesEnRango(fechaInicio, fechaFin));
    }

    @Operation(summary = "¿Está libre una herramienta en esas fechas?",
            description = "**Roles:** cualquier usuario autenticado. Útil antes de reservar.")
    @ApiResponse(responseCode = "200", description = "Resultado de la consulta")
    @ApiResponse(responseCode = "400", description = "La fecha de inicio no puede ser posterior a la fecha fin.")
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @GetMapping("/disponibilidad/{herramientaId}")
    public ResponseEntity<DisponibilidadResponse> disponible(
            @Parameter(example = "1") @PathVariable Long herramientaId,
            @Parameter(description = "Primer día (yyyy-MM-dd)", example = "2026-11-10") @RequestParam LocalDate fechaInicio,
            @Parameter(description = "Último día, incluido (yyyy-MM-dd)", example = "2026-11-12") @RequestParam LocalDate fechaFin
    ) {
        boolean disponible = consultarDisponibilidadUseCase.estaDisponible(herramientaId, fechaInicio, fechaFin);
        return ResponseEntity.ok(new DisponibilidadResponse(herramientaId, fechaInicio, fechaFin, disponible));
    }

    /** clienteId es opcional: solo lo usa un ADMIN que reserva en nombre de un cliente. */
    public record ReservaRequest(
            @Schema(example = "12", description = "Solo para ADMIN: cliente para quien se reserva")
            Long clienteId,
            @Schema(example = "1")
            @NotNull(message = "La herramienta es obligatoria.")
            Long herramientaId,
            // Que no sea pasada lo valida CrearReservaService con el reloj de app.zona-horaria.
            @Schema(example = "2026-11-10", description = "Primer día del alquiler (no puede ser pasado)")
            @NotNull(message = "La fecha de inicio es obligatoria.")
            LocalDate fechaInicio,
            @Schema(example = "2026-11-12", description = "Último día del alquiler (incluido)")
            @NotNull(message = "La fecha fin es obligatoria.")
            LocalDate fechaFin
    ) {}

    public record ModificarReservaRequest(
            @Schema(example = "2026-11-15")
            @NotNull(message = "La fecha de inicio es obligatoria.")
            LocalDate fechaInicio,
            @Schema(example = "2026-11-17")
            @NotNull(message = "La fecha fin es obligatoria.")
            LocalDate fechaFin
    ) {}

    public record DevolucionRequest(
            @Schema(example = "DANADO", allowableValues = {"BUENO", "DANADO", "PERDIDO"}, description = "También acepta \"DAÑADO\"")
            @NotBlank(message = "El estado del equipo es obligatorio (BUENO, DANADO o PERDIDO).")
            String estadoEquipo,
            @Schema(example = "El cable de alimentación llegó pelado")
            @Size(max = 1000, message = "Las observaciones admiten máximo 1000 caracteres.")
            String observaciones
    ) {}

    public record DisponibilidadResponse(
            @Schema(example = "1") Long herramientaId,
            @Schema(example = "2026-11-10") LocalDate fechaInicio,
            @Schema(example = "2026-11-12") LocalDate fechaFin,
            @Schema(example = "true") boolean disponible
    ) {}
}
