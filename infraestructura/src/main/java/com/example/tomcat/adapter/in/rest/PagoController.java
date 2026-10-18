package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.GestionarPagoUseCase;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.port.in.ProcesarPagoUseCase;
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
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Pagos")
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final ProcesarPagoUseCase procesarPagoUseCase;
    private final ListarPagosUseCase listarPagosUseCase;
    private final GestionarPagoUseCase gestionarPagoUseCase;

    public PagoController(ProcesarPagoUseCase procesarPagoUseCase, ListarPagosUseCase listarPagosUseCase,
                          GestionarPagoUseCase gestionarPagoUseCase) {
        this.procesarPagoUseCase = procesarPagoUseCase;
        this.listarPagosUseCase = listarPagosUseCase;
        this.gestionarPagoUseCase = gestionarPagoUseCase;
    }

    @Operation(summary = "Pagar una reserva en línea",
            description = "**Roles:** el CLIENTE que hizo la reserva o ADMIN. La reserva debe estar **ACEPTADA** y el monto "
                    + "debe ser exactamente `tarifaDiaria × días` (ambos extremos incluidos). En una sola operación: se registra "
                    + "el pago, la reserva pasa a **CONFIRMADA** (confirmación automática) y se genera la factura "
                    + "(`GET /api/facturas/pago/{pagoId}`). El cliente tiene 1 hora desde la aceptación para pagar.")
    @ApiResponse(responseCode = "201", description = "Pago registrado, reserva confirmada y factura generada")
    @ApiResponse(responseCode = "400", description = "El monto del pago debe ser 135000.00 según tarifa y días de alquiler.")
    @ApiResponse(responseCode = "403", description = "Solo el cliente que hizo la reserva puede pagarla.")
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @ApiResponse(responseCode = "409", description = "La reserva ya tiene un pago completado.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<Pago> pagar(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procesarPagoUseCase.procesar(
                usuario.solicitante(), request.reservaId(), request.monto(), request.metodoPago()));
    }

    @Operation(summary = "Historial de pagos",
            description = "**Roles:** CLIENTE (sus pagos), PROVEEDOR (pagos de reservas de sus herramientas) o ADMIN (todos).")
    @ApiResponse(responseCode = "200", description = "Pagos visibles para el usuario")
    @ApiResponse(responseCode = "403", description = "No tiene permiso para acceder a esta reserva.")
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @GetMapping
    public ResponseEntity<List<Pago>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Parameter(description = "Solo pagos de esta reserva", example = "1")
                                             @RequestParam(required = false) Long reservaId) {
        if (reservaId != null) {
            return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(usuario.solicitante(), reservaId));
        }
        return ResponseEntity.ok(listarPagosUseCase.listar(usuario.solicitante()));
    }

    @Operation(summary = "Pagos de una reserva", description = "**Roles:** el CLIENTE de la reserva, el PROVEEDOR de la herramienta o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Pagos de la reserva")
    @ApiResponse(responseCode = "403", description = "No tiene permiso para acceder a esta reserva.")
    @ApiResponse(responseCode = "404", description = "La reserva no existe.")
    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<Pago>> historialPorReserva(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                          @Parameter(example = "1") @PathVariable Long reservaId) {
        return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(usuario.solicitante(), reservaId));
    }

    @Operation(summary = "Consultar un pago", description = "**Roles:** el CLIENTE de la reserva, el PROVEEDOR de la herramienta o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Pago encontrado")
    @ApiResponse(responseCode = "403", description = "No tiene permiso para acceder a esta reserva.")
    @ApiResponse(responseCode = "404", description = "El pago no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<Pago> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(listarPagosUseCase.buscarPorId(usuario.solicitante(), id));
    }

    /** Solo ADMIN: corrige el método de pago registrado (y el de su factura). */
    @Operation(summary = "Corregir el método de un pago",
            description = "**Rol:** ADMIN. Solo cambia el método (el monto lo fija la tarifa); también se corrige en la factura.")
    @ApiResponse(responseCode = "200", description = "Pago actualizado")
    @ApiResponse(responseCode = "400", description = "Método de pago no válido. Use TARJETA, TRANSFERENCIA o EFECTIVO.")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @ApiResponse(responseCode = "404", description = "El pago no existe.")
    @PutMapping("/{id}")
    public ResponseEntity<Pago> actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @Parameter(example = "1") @PathVariable Long id,
                                           @Valid @RequestBody ActualizarPagoRequest request) {
        return ResponseEntity.ok(gestionarPagoUseCase.actualizarMetodo(usuario.solicitante(), id, request.metodoPago()));
    }

    /**
     * Solo ADMIN: reembolsa el pago de una reserva aún no entregada. No se borra: el pago queda REEMBOLSADO,
     * la reserva CANCELADA y la factura ANULADA.
     */
    @Operation(summary = "Reembolsar un pago",
            description = "**Rol:** ADMIN. Solo pagos COMPLETADO de reservas que aún no se entregan. No se borra nada: "
                    + "el pago queda **REEMBOLSADO** (deja de contar como ingreso), la reserva **CANCELADA** (libera las fechas) "
                    + "y la factura **ANULADA**.")
    @ApiResponse(responseCode = "200", description = "Pago reembolsado")
    @ApiResponse(responseCode = "400", description = "Solo se pueden reembolsar pagos completados.")
    @ApiResponse(responseCode = "403", description = "Su rol no tiene permiso para realizar esta operación.")
    @ApiResponse(responseCode = "404", description = "El pago no existe.")
    @ApiResponse(responseCode = "409", description = "La herramienta ya fue entregada: el pago no se puede reembolsar.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Pago> reembolsar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @Parameter(example = "5") @PathVariable Long id) {
        return ResponseEntity.ok(gestionarPagoUseCase.reembolsar(usuario.solicitante(), id));
    }

    public record ActualizarPagoRequest(
            @Schema(example = "TRANSFERENCIA", allowableValues = {"TARJETA", "TRANSFERENCIA", "EFECTIVO"})
            @NotBlank(message = "El método de pago es obligatorio (TARJETA, TRANSFERENCIA o EFECTIVO).")
            String metodoPago
    ) {}

    public record PagoRequest(
            @Schema(example = "35")
            @NotNull(message = "La reserva es obligatoria.")
            Long reservaId,
            @Schema(example = "135000", description = "Debe ser tarifaDiaria × días de la reserva")
            @NotNull(message = "El monto es obligatorio.")
            @Positive(message = "El monto debe ser mayor que cero.")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 10 enteros y 2 decimales.")
            BigDecimal monto,
            @Schema(example = "TARJETA", allowableValues = {"TARJETA", "TRANSFERENCIA", "EFECTIVO"})
            @NotBlank(message = "El método de pago es obligatorio (TARJETA, TRANSFERENCIA o EFECTIVO).")
            String metodoPago
    ) {}
}
