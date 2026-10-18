package com.example.tomcat.adapter.in.rest;

import com.example.tomcat.model.Pago;
import com.example.tomcat.port.in.GestionarPagoUseCase;
import com.example.tomcat.port.in.ListarPagosUseCase;
import com.example.tomcat.port.in.ProcesarPagoUseCase;
import com.example.tomcat.security.UsuarioAutenticado;
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

    @PostMapping
    public ResponseEntity<Pago> pagar(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procesarPagoUseCase.procesar(
                usuario.solicitante(), request.reservaId(), request.monto(), request.metodoPago()));
    }

    @GetMapping
    public ResponseEntity<List<Pago>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @RequestParam(required = false) Long reservaId) {
        if (reservaId != null) {
            return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(usuario.solicitante(), reservaId));
        }
        return ResponseEntity.ok(listarPagosUseCase.listar(usuario.solicitante()));
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<Pago>> historialPorReserva(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                          @PathVariable Long reservaId) {
        return ResponseEntity.ok(listarPagosUseCase.historialPorReserva(usuario.solicitante(), reservaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> obtener(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(listarPagosUseCase.buscarPorId(usuario.solicitante(), id));
    }

    /** Solo ADMIN: corrige el método de pago registrado (y el de su factura). */
    @PutMapping("/{id}")
    public ResponseEntity<Pago> actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ActualizarPagoRequest request) {
        return ResponseEntity.ok(gestionarPagoUseCase.actualizarMetodo(usuario.solicitante(), id, request.metodoPago()));
    }

    /**
     * Solo ADMIN: reembolsa el pago de una reserva aún no entregada. No se borra: el pago queda REEMBOLSADO,
     * la reserva CANCELADA y la factura ANULADA.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Pago> reembolsar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return ResponseEntity.ok(gestionarPagoUseCase.reembolsar(usuario.solicitante(), id));
    }

    public record ActualizarPagoRequest(
            @NotBlank(message = "El método de pago es obligatorio (TARJETA, TRANSFERENCIA o EFECTIVO).")
            String metodoPago
    ) {}

    public record PagoRequest(
            @NotNull(message = "La reserva es obligatoria.")
            Long reservaId,
            @NotNull(message = "El monto es obligatorio.")
            @Positive(message = "El monto debe ser mayor que cero.")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 10 enteros y 2 decimales.")
            BigDecimal monto,
            @NotBlank(message = "El método de pago es obligatorio (TARJETA, TRANSFERENCIA o EFECTIVO).")
            String metodoPago
    ) {}
}
