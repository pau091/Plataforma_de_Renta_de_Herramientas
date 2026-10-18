package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Mantenimiento;
import com.example.tomcat.port.in.GestionarMantenimientoUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** ADMIN o PROVEEDOR (solo los de sus herramientas). */
@Tag(name = "Mantenimientos")
@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private static final String SOLO_DUENO = "Solo el proveedor dueño de la herramienta puede gestionarla.";

    private final GestionarMantenimientoUseCase gestionarMantenimientoUseCase;

    public MantenimientoController(GestionarMantenimientoUseCase gestionarMantenimientoUseCase) {
        this.gestionarMantenimientoUseCase = gestionarMantenimientoUseCase;
    }

    @Operation(summary = "Listar mantenimientos",
            description = "**Roles:** PROVEEDOR (los de sus herramientas) o ADMIN (todos). Un mantenimiento con "
                    + "`fechaFin` nula está abierto.")
    @ApiResponse(responseCode = "200", description = "Mantenimientos visibles para el usuario")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @GetMapping
    public ResponseEntity<List<Mantenimiento>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Solo los de esta herramienta", example = "4")
            @RequestParam(required = false) Long herramientaId,
            @Parameter(description = "Solo el generado por la devolución de esta reserva", example = "4")
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

    @Operation(summary = "Abrir un mantenimiento",
            description = "**Roles:** el PROVEEDOR dueño o ADMIN. La herramienta pasa a MANTENIMIENTO (no se puede reservar). "
                    + "Solo puede haber uno abierto por herramienta.")
    @ApiResponse(responseCode = "201", description = "Mantenimiento abierto")
    @ApiResponse(responseCode = "400", description = "La descripción del problema es obligatoria.")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "La herramienta no existe.")
    @ApiResponse(responseCode = "409", description = "La herramienta ya tiene un mantenimiento abierto.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<Mantenimiento> registrar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   @Valid @RequestBody MantenimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gestionarMantenimientoUseCase.registrar(
                usuario.solicitante(),
                request.herramientaId(),
                request.descripcionProblema(),
                request.costoReparacion()
        ));
    }

    @Operation(summary = "Cerrar un mantenimiento",
            description = "**Roles:** el PROVEEDOR dueño o ADMIN. Registra el costo final; la herramienta vuelve a "
                    + "DISPONIBLE solo si seguía en MANTENIMIENTO (una FUERA_DE_SERVICIO no se reactiva).")
    @ApiResponse(responseCode = "200", description = "Mantenimiento cerrado")
    @ApiResponse(responseCode = "400", description = "El mantenimiento ya fue cerrado.")
    @ApiResponse(responseCode = "403", description = SOLO_DUENO)
    @ApiResponse(responseCode = "404", description = "El mantenimiento no existe.")
    @PostMapping("/{id}/cerrar")
    public ResponseEntity<Mantenimiento> cerrar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @Parameter(example = "21") @PathVariable Long id,
                                                @Valid @RequestBody CerrarRequest request) {
        return ResponseEntity.ok(gestionarMantenimientoUseCase.cerrar(usuario.solicitante(), id, request.costoReparacion()));
    }

    public record MantenimientoRequest(
            @Schema(example = "1")
            @NotNull(message = "La herramienta es obligatoria.")
            Long herramientaId,
            @Schema(example = "Cambio de carbones del motor")
            @NotBlank(message = "La descripción del problema es obligatoria.")
            String descripcionProblema,
            @Schema(example = "15000", description = "Opcional: costo estimado (se puede fijar al cerrar)")
            @PositiveOrZero(message = "El costo de reparación no puede ser negativo.")
            @Digits(integer = 10, fraction = 2, message = "El costo admite hasta 10 enteros y 2 decimales.")
            BigDecimal costoReparacion
    ) {}

    public record CerrarRequest(
            @Schema(example = "120000", description = "Costo final de la reparación")
            @PositiveOrZero(message = "El costo de reparación no puede ser negativo.")
            @Digits(integer = 10, fraction = 2, message = "El costo admite hasta 10 enteros y 2 decimales.")
            BigDecimal costoReparacion
    ) {}
}
